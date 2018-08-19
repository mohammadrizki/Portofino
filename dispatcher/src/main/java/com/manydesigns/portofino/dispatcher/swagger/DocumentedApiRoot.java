package com.manydesigns.portofino.dispatcher.swagger;

import com.manydesigns.portofino.dispatcher.Node;
import com.manydesigns.portofino.dispatcher.NodeWithParameters;
import com.manydesigns.portofino.dispatcher.Root;
import com.manydesigns.portofino.dispatcher.RootFactory;
import com.manydesigns.portofino.dispatcher.visitor.DepthFirstVisitor;
import com.manydesigns.portofino.dispatcher.visitor.NodeVisitor;
import io.swagger.annotations.Api;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ResourceContext;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by alessio on 28/07/16.
 */
@Api
public abstract class DocumentedApiRoot implements ReaderListener {
    
    protected static final Logger logger = LoggerFactory.getLogger(DocumentedApiRoot.class);
    protected static RootFactory rootFactory;

    public static void setRootFactory(RootFactory rootFactory) {
        DocumentedApiRoot.rootFactory = rootFactory;
    }

    public static RootFactory getRootFactory() {
        return rootFactory;
    }
    
    @Override
    public void beforeScan(Reader reader, Swagger swagger) {

    }

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        final SubResourceReader subResourceReader = new SubResourceReader(reader);
        try {
            Root root = rootFactory.createRoot();
            root.setResourceContext(new DummyResourceContext());
            new DepthFirstVisitor((NodeVisitor) node -> {
                try {
                    subResourceReader.readSubResource(node);
                } catch (Exception e) {
                    logger.error("Could not read node at " + node.getLocation(), e);
                }
            }).visit(root);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    static class SubResourceReader extends Reader {

        public SubResourceReader(Reader reader) {
            super(reader.getSwagger(), reader.getConfig());
        }

        public Swagger readSubResource(Node node) {
            String path = node.getPath();
            ArrayList<Parameter> parameters = new ArrayList<>();
            int paramCount = 0;
            while (path.contains("{requiredPathParameter}")) {
                String name = "requiredPathParameter_" + paramCount;
                path = path.replaceFirst("\\{requiredPathParameter}", "{" + name + "}");
                PathParameter parameter = new PathParameter();
                parameter.setName(name);
                parameter.setRequired(true);
                parameters.add(parameter);
                paramCount++;
            }
            paramCount = 0;
            while (path.contains("{optionalPathParameter}")) {
                String name = "optionalPathParameter_" + paramCount;
                path = path.replaceFirst("\\{optionalPathParameter}", "{" + name + "}");
                PathParameter parameter = new PathParameter();
                parameter.setName(name);
                parameter.setRequired(false);
                parameters.add(parameter);
                paramCount++;
            }

            return read(node.getClass(), path, null, true, new String[0], new String[0],
                    new HashMap<>(), parameters);
        }
    }

    protected static class DummyResourceContext implements ResourceContext {
        @Override
        public <T> T getResource(Class<T> resourceClass) {
            try {
                return initResource(resourceClass.newInstance());
            } catch (Exception e) {
                logger.warn("Could not create resource for Swagger", e);
                return null;
            }
        }

        @Override
        public <T> T initResource(T resource) {
            if(resource instanceof Node) {
                ((Node) resource).setResourceContext(this);
            }
            return resource;
        }
    }
    
}
