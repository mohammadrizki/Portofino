/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
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

package com.manydesigns.elements.forms;

import com.manydesigns.elements.annotations.FieldSet;
import com.manydesigns.elements.fields.Field;
import com.manydesigns.elements.fields.OptionProvider;
import com.manydesigns.elements.fields.SelectField;
import com.manydesigns.elements.fields.helpers.FieldsManager;
import com.manydesigns.elements.logging.LogUtil;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.JavaClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;
import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/
public class FormBuilder {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    public final static int DEFAULT_N_COLUMNS = 1;
    public final static String[] PROPERTY_NAME_BLACKLIST = {"class"};

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected FieldsManager manager;

    protected final ClassAccessor classAccessor;

    protected List<ArrayList<PropertyAccessor>> groupedPropertyAccessors;
    protected List<String> fieldSetNames;
    protected final Map<String[], OptionProvider> optionProviders;
    protected String prefix;
    protected int nColumns = DEFAULT_N_COLUMNS;

    public static final Logger logger = LogUtil.getLogger(FormBuilder.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public FormBuilder(Class aClass) {
        this(JavaClassAccessor.getClassAccessor(aClass));
    }

    public FormBuilder(ClassAccessor classAccessor) {
        LogUtil.entering(logger, "FormBuilder", classAccessor);

        manager = FieldsManager.getManager();
        this.classAccessor = classAccessor;
        optionProviders = new HashMap<String[], OptionProvider>();

        LogUtil.exiting(logger, "FormBuilder");
    }

    //**************************************************************************
    // Builder configuration
    //**************************************************************************

    public FormBuilder configFields(String... fieldNames) {
        LogUtil.fineMF(logger, "Configuring fields to: {0}", fieldNames);

        String[][] groupedFieldNames = new String[1][];
        groupedFieldNames[0] = fieldNames;
        return configFields(groupedFieldNames);
    }

    public FormBuilder configFields(String[]... groupedFieldNames) {
        LogUtil.entering(logger, "configFields", groupedFieldNames);

        groupedPropertyAccessors = new ArrayList<ArrayList<PropertyAccessor>>();
        for (String[] currentNameGroup : groupedFieldNames) {
            ArrayList<PropertyAccessor> currentPropertyGroup =
                    new ArrayList<PropertyAccessor>();
            groupedPropertyAccessors.add(currentPropertyGroup);
            for (String currentField : currentNameGroup) {
                try {
                    PropertyAccessor accessor =
                            classAccessor.getProperty(currentField);
                    currentPropertyGroup.add(accessor);
                } catch (NoSuchFieldException e) {
                    LogUtil.warningMF(logger, "Field not found: {0}", e,
                            currentField);
                }
            }
        }

        LogUtil.exiting(logger, "configFields");
        return this;
    }

    public FormBuilder configPrefix(String prefix) {
        LogUtil.fineMF(logger, "Configuring prefix to: {0}", prefix);

        this.prefix = prefix;
        return this;
    }

    public FormBuilder configNColumns(int nColumns) {
        LogUtil.fineMF(logger, "Configuring nColumns to: {0}", nColumns);

        this.nColumns = nColumns;
        return this;
    }

    public FormBuilder configFieldSetNames(String... fieldSetNames) {
        LogUtil.fineMF(logger,
                "Configuring configFieldSetNames to: {0}", fieldSetNames);

        this.fieldSetNames = Arrays.asList(fieldSetNames);
        return this;
    }

    public FormBuilder configOptionProvider(OptionProvider optionProvider,
                                            String... fieldNames) {
        optionProviders.put(fieldNames, optionProvider);
        return this;
    }

    public FormBuilder configReflectiveFields() {
        LogUtil.entering(logger, "configReflectiveFields");

        List<String> blackList = Arrays.asList(PROPERTY_NAME_BLACKLIST);
        groupedPropertyAccessors = new ArrayList<ArrayList<PropertyAccessor>>();
        fieldSetNames = new ArrayList<String>();

        ArrayList<PropertyAccessor> currentGroup = null;
        String currentGroupName = null;
        for (PropertyAccessor current : classAccessor.getProperties()) {
            if (Modifier.isStatic(current.getModifiers())) {
                continue;
            }
            if (blackList.contains(current.getName())) {
                continue;
            }

            String groupName = null;
            if (current.isAnnotationPresent(
                    com.manydesigns.elements.annotations.FieldSet.class)) {
                groupName = current.getAnnotation(FieldSet.class).value();
            }

            if ((currentGroup == null)
                    || (groupName != null && !groupName.equals(currentGroupName))) {
                currentGroup = new ArrayList<PropertyAccessor>();
                groupedPropertyAccessors.add(currentGroup);
                fieldSetNames.add(groupName);
                currentGroupName = groupName;
            }
            currentGroup.add(current);
        }

        LogUtil.exiting(logger, "configReflectiveFields");
        return this;
    }

    //**************************************************************************
    // Building
    //**************************************************************************

    public Form build() {
        LogUtil.entering(logger, "build");

        Form form = new Form();

        if (groupedPropertyAccessors == null) {
            configReflectiveFields();
        }

        // create the form/fieldset/field sructure
        Map<String,Field> fieldMap = new HashMap<String,Field>();
        for (int i = 0; i < groupedPropertyAccessors.size(); i++) {
            buildFieldGroup(form, i, fieldMap);
        }

        // bind chained select fields
        for (String[] fieldNames : optionProviders.keySet()) {
            for (int i = 1; i < fieldNames.length; i++) {
                SelectField field =
                        (SelectField) fieldMap.get(fieldNames[i]);
                SelectField previousField =
                        (SelectField) fieldMap.get(fieldNames[i-1]);
                field.setPreviousSelectField(previousField);
                previousField.setNextSelectField(field);
            }
        }

        LogUtil.exiting(logger, "build");
        return form;
    }

    protected void buildFieldGroup(Form form,
                                   int i,
                                   Map<String,Field> fieldMap) {
        ArrayList<PropertyAccessor> group = groupedPropertyAccessors.get(i);
        String fieldSetName;
        if (fieldSetNames == null) {
            fieldSetName = null;
        } else {
            fieldSetName = fieldSetNames.get(i);
        }
        com.manydesigns.elements.forms.FieldSet fieldSet =
                new com.manydesigns.elements.forms.FieldSet(
                        fieldSetName, nColumns);
        form.add(fieldSet);
        for (PropertyAccessor propertyAccessor : group) {
            buildField(fieldSet, propertyAccessor, fieldMap);
        }
    }

    protected void buildField(com.manydesigns.elements.forms.FieldSet fieldSet,
                              PropertyAccessor propertyAccessor,
                              Map<String,Field> fieldMap) {
        Field field = null;
        String fieldName = propertyAccessor.getName();
        for (Map.Entry<String[], OptionProvider> current
                : optionProviders.entrySet()) {
            String[] fieldNames = current.getKey();
            OptionProvider optionProvider = current.getValue();
            int index = ArrayUtils.indexOf(fieldNames, fieldName);
            if (index >= 0) {
                SelectField selectField =
                        new SelectField(propertyAccessor, prefix);
                selectField.setOptionProvider(optionProvider);
                selectField.setOptionProviderIndex(index);
                field = selectField;
                break;
            }
        }
        if (field == null) {
            field = manager.tryToInstantiateField(
                    classAccessor, propertyAccessor, prefix);
        }

        if (field == null) {
            LogUtil.warningMF(logger,
                    "Cannot instanciate field for property {0}",
                    propertyAccessor);
            return;
        }

        fieldSet.add(field);
        fieldMap.put(fieldName, field);
    }
}
