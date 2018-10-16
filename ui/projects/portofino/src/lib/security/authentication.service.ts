import {Inject, Injectable, InjectionToken} from '@angular/core';
import {
  HttpClient,
  HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpParams, HttpRequest
} from "@angular/common/http";
import {TokenStorageService} from "./token-storage.service";
import {MatDialog, MatDialogRef} from "@angular/material";
import {Observable, throwError} from "rxjs";
import { catchError, map, mergeMap } from "rxjs/operators";
import {PortofinoService} from "../portofino.service";
import {Router} from "@angular/router";

export const LOGIN_COMPONENT = new InjectionToken('Login Component');

@Injectable()
export class AuthenticationService {

  dialogRef: MatDialogRef<any>;
  currentUser: UserInfo;
  retryUnauthenticatedOnSessionExpiration = true;

  constructor(private http: HttpClient, protected dialog: MatDialog, protected storage: TokenStorageService,
              private portofino: PortofinoService, @Inject(LOGIN_COMPONENT) protected loginComponent,
              protected router: Router) {
    const displayName = this.storage.get('user.displayName');
    if(displayName) {
      this.currentUser = new UserInfo(displayName, this.storage.get('user.administrator') == 'true');
    }
  }

  request(req: HttpRequest<any>, observable: Observable<HttpEvent<any>>): Observable<HttpEvent<any>> {
    return observable.pipe(catchError((error) => {
      if (error.status === 401) {
        const hasToken = !!this.jsonWebToken;
        this.removeAuthenticationInfo();
        if(hasToken || !this.retryUnauthenticatedOnSessionExpiration) {
          req = req.clone({ headers: req.headers.delete("Authorization") });
          return this.doHttpRequest(req).pipe(map(result => {
            alert("You have been logged out because your session has expired."); //TODO proper dialog/alert
            return result;
          }));
        } else {
          return this.askForCredentials().pipe(
            map(result => {
              if (!result) {
                throw new Error("User declined login");
              }
            }),
            mergeMap(_ => this.doHttpRequest(this.withAuthenticationHeader(req))));
        }
      } else if(error.status === 403) {
        alert("You do not have the permission to do that!"); //TODO proper dialog/alert
      }
      return throwError(error);
    }));
  }

  protected doHttpRequest(req) {
    return this.http.request(req);
  }

  protected askForCredentials() {
    if(!this.dialogRef) {
      this.dialogRef = this.dialog.open(this.loginComponent);
    }
    return this.dialogRef.afterClosed().pipe(map(result => {
      this.dialogRef = null;
      if (result && result.jwt) {
        this.setAuthenticationInfo(result);
        return result;
      } else {
        return null;
      }
    }));
  }

  public showLoginDialog() {
    this.askForCredentials().subscribe(result => {
      if(result) {
        this.router.navigateByUrl(this.router.url);
      }
    });
  }

  protected removeAuthenticationInfo() {
    this.storage.remove('jwt');
    this.storage.remove('user.displayName');
    this.storage.remove('user.administrator');
    this.storage.remove('sessionId');
    this.currentUser = null;
  }

  protected setAuthenticationInfo(result) {
    this.storage.set('jwt', result.jwt);
    this.storage.set('user.displayName', result.displayName);
    this.storage.set('user.administrator', result.administrator);
    this.storage.set('sessionId', result.portofinoSessionId);
    this.currentUser = new UserInfo(result.displayName, result.administrator);
  }

  withAuthenticationHeader(req: HttpRequest<any>) {
    if(!this.jsonWebToken) {
      return req;
    }
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${this.jsonWebToken}`
      }
    });
    return req;
  }

  public get jsonWebToken() {
    return this.storage.get('jwt');
  }

  login(username, password) {
    const headers = new HttpHeaders()
      .set('Content-Type', 'application/x-www-form-urlencoded')
      .set(NO_AUTH_HEADER, 'true');
    return this.http.post(
      this.loginPath,
      new HttpParams({fromObject: {"username": username, "password": password}}),
      {headers: headers}
    );
  }

  get loginPath() {
    return `${this.portofino.apiRoot}${this.portofino.loginPath}`;
  }

  logout() {
    const url = `${this.loginPath}/${this.storage.get('sessionId')}`;
    this.http.delete(url).subscribe(value => {
      this.removeAuthenticationInfo();
      this.router.navigateByUrl(this.router.url);
    });
  }

  get isAdmin() {
    return this.currentUser && this.currentUser.administrator
  }
}

@Injectable()
export class AuthenticationInterceptor implements HttpInterceptor {

  constructor(protected authenticationService: AuthenticationService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    req = this.authenticationService.withAuthenticationHeader(req);
    if(req.headers.has(NO_AUTH_HEADER)) {
      req = req.clone({ headers: req.headers.delete(NO_AUTH_HEADER) });
      return next.handle(req);
    } else {
      return this.authenticationService.request(req, next.handle(req));
    }
  }
}

export class UserInfo {
  constructor(public displayName: string, public administrator: boolean) {}
}

export const NO_AUTH_HEADER = "portofino-no-auth";
