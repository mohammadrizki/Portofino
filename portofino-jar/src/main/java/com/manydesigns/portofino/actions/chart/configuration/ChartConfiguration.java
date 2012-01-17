/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.actions.chart.configuration;

import com.manydesigns.elements.annotations.*;
import com.manydesigns.portofino.chart.*;
import com.manydesigns.portofino.logic.DataModelLogic;
import com.manydesigns.portofino.model.Model;
import com.manydesigns.portofino.model.ModelObject;
import com.manydesigns.portofino.model.ModelVisitor;
import com.manydesigns.portofino.model.datamodel.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class ChartConfiguration implements ModelObject {
    public static final String copyright =
            "Copyright (c) 2005-2011, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected String name;
    protected String type;
    protected String legend;
    protected String database;
    protected String query;
    protected String urlExpression;
    protected String xAxisName;
    protected String yAxisName;
    protected String orientation;

    //**************************************************************************
    // Fields for wire-up
    //**************************************************************************

    protected Database actualDatabase;
    protected Type actualType;
    protected Class<? extends ChartGenerator> generatorClass;
    protected Orientation actualOrientation;

    public static enum Orientation {
        HORIZONTAL, VERTICAL
    }

    //**************************************************************************
    // Built-in chart generators
    //**************************************************************************

    public static enum Type {
        AREA(ChartAreaGenerator.class),
        BAR(ChartBarGenerator.class),
        BAR3D(ChartBar3DGenerator.class),
        LINE(ChartLineGenerator.class),
        LINE3D(ChartLine3DGenerator.class),
        PIE(ChartPieGenerator.class),
        PIE3D(ChartPie3DGenerator.class),
        RING(ChartRingGenerator.class),
        STACKED_BAR(ChartStackedBarGenerator.class),
        STACKED_BAR_3D(ChartStackedBar3DGenerator.class);

        private Class<? extends ChartGenerator> generatorClass;

        Type(Class<? extends ChartGenerator> generatorClass) {
            this.generatorClass = generatorClass;
        }

        public Class<? extends ChartGenerator> getGeneratorClass() {
            return generatorClass;
        }
    }

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(ChartConfiguration.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************


    public ChartConfiguration() {
        super();
    }

    //**************************************************************************
    // ModelObject implementation
    //**************************************************************************

    public void reset() {
        actualDatabase = null;
        generatorClass = null;
        actualOrientation = null;
        actualType = null;
    }

    public void init(Model model) {
        assert name != null;
        assert type != null;
        assert legend != null;
        assert database != null;
        assert query != null;
        assert urlExpression != null;

        try {
            actualType = Type.valueOf(type);
            generatorClass = actualType.getGeneratorClass();
        } catch (Exception e) {
            logger.error("Invalid chart type: " + type, e);
        }

        if(orientation != null) {
            try {
                actualOrientation = Orientation.valueOf(orientation);
            } catch (Exception e) {
                logger.error("Invalid orientation: " + actualOrientation, e);
            }
        }
    }

    public void afterUnmarshal(Unmarshaller u, Object parent) {
    }

    public void visitChildren(ModelVisitor visitor) {
    }

    public void link(Model model) {
        actualDatabase = DataModelLogic.findDatabaseByName(model, database);
    }

    public String getQualifiedName() {
        return null;
    }

    //**************************************************************************
    // Getters/setters
    //**************************************************************************
    @Required
    @XmlAttribute(required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "type", required = true)
    @Required
    @Label("Type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Type getActualType() {
        return actualType;
    }

    @Required
    @XmlAttribute(required = true)
    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }

    @Required
    @XmlAttribute(required = true)
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Required
    @Label("SQL Query")
    @Multiline
    @XmlAttribute(required = true)
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Label("URL expression")
    @XmlAttribute(required = true)
    @FieldSize(100)
    public String getUrlExpression() {
        return urlExpression;
    }

    public void setUrlExpression(String urlExpression) {
        this.urlExpression = urlExpression;
    }

    public Database getActualDatabase() {
        return actualDatabase;
    }

    public Class<? extends ChartGenerator> getGeneratorClass() {
        return generatorClass;
    }

    @XmlAttribute(name = "xAxisName")
    public String getXAxisName() {
        return xAxisName;
    }

    public void setXAxisName(String xAxisName) {
        this.xAxisName = xAxisName;
    }

    @XmlAttribute(name = "yAxisName")
    public String getYAxisName() {
        return yAxisName;
    }

    public void setYAxisName(String yAxisName) {
        this.yAxisName = yAxisName;
    }

    @XmlAttribute(name = "orientation")
    @Label("Orientation")
    @Required
    @Select(nullOption = false)
    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public Orientation getActualOrientation() {
        return actualOrientation;
    }
}
