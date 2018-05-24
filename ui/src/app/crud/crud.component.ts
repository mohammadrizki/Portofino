import {Component, Input, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {PortofinoService} from "../portofino.service";
import {ClassAccessor, isEnabled, isInSummary, isSearchable, isUpdatable, Property} from "../class-accessor";

@Component({
  selector: 'portofino-crud',
  templateUrl: './crud.component.html',
  styleUrls: ['./crud.component.css']
})
export class CrudComponent implements OnInit {

  @Input()
  config: any;

  configuration: Configuration;
  classAccessor: ClassAccessor;
  classAccessorPath = '/:classAccessor';
  configurationPath = '/:configuration';

  @Input()
  pageSize: number;

  searchVisible = false;
  createVisible = false;
  editVisible = false;

  id: string;

  createProperties: Property[] = [];

  constructor(private http: HttpClient, public portofino: PortofinoService) { }

  ngOnInit() {
    const baseUrl = this.portofino.apiPath + this.config.path;
    this.http.get<ClassAccessor>(baseUrl + this.classAccessorPath).subscribe(
      classAccessor => this.http.get<Configuration>(baseUrl + this.configurationPath).subscribe(
        configuration => this.init(classAccessor, configuration)
      )
    );
  }

  protected init(classAccessor, configuration) {
    this.classAccessor = classAccessor;
    this.configuration = {...configuration, ...this.config};
    this.classAccessor.properties.forEach(p => {
      p.key = (this.classAccessor.keyProperties.find(k => k == p.name) != null);
    });
    this.searchVisible = true;
  }

  createNew() {
    this.searchVisible = false;
    this.editVisible = false;
    this.createVisible = true;
  }

  openDetail(id: string) {
    this.id = id;
    this.searchVisible = false;
    this.editVisible = true;
    this.createVisible = false;
  }

  closeDetail() {
    this.searchVisible = true;
    this.createVisible = false;
    this.editVisible = false;
    this.id = null;
  }
}

export class Configuration {
  rowsPerPage: number;
  path: string;
}
