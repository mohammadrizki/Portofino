package com.manydesigns.portofino.actions.admin.appwizard

import com.manydesigns.elements.reflection.ClassAccessor
import com.manydesigns.portofino.model.database.DatabaseLogic
import com.manydesigns.portofino.model.database.Table
import com.manydesigns.portofino.pageactions.calendar.CalendarAction
import com.manydesigns.portofino.pageactions.calendar.Event
import com.manydesigns.portofino.reflection.TableAccessor
import com.manydesigns.portofino.security.AccessLevel
import com.manydesigns.portofino.security.RequiresPermissions
import com.manydesigns.portofino.util.ShortNameUtils
import java.awt.Color
import org.hibernate.Criteria
import org.hibernate.Session
import org.hibernate.criterion.Restrictions
import org.joda.time.DateTime
import org.joda.time.Interval
import com.manydesigns.portofino.pageactions.calendar.Calendar
import org.hibernate.criterion.Order
import org.joda.time.DateTimeZone
import com.manydesigns.elements.util.Util

@RequiresPermissions(level = AccessLevel.VIEW)
class CalendarPage extends CalendarAction {

    def calDefs = $calendarDefinitions; //Es. [["Cal 1", "db1.schema1.table1", ["column1", "column2"], Color.RED], ["Cal 2", "db1.schema1.table2", ["column1", "column2"], Color.BLUE]]

    @Override
    void loadObjects(Interval interval) {
        for(cal in calDefs) {
            Table table = DatabaseLogic.findTableByQualifiedName(model, cal[1])
            Session session = application.getSession(table.schema.databaseName)
            def objects = []
            for(col in cal[2]) {
                Criteria criteria = session.createCriteria(table.actualEntityName)
                criteria.add(Restrictions.ge(col, interval.start.toDate()))
                criteria.add(Restrictions.lt(col, interval.end.toDate()))
                objects.add([col, criteria.list()])
            }
            addCalendar(cal, table, objects)
        }
    }

    protected def addCalendar(calDef, Table table, List data) {
        Calendar calendar = new Calendar(calDef[1], calDef[0], calDef[3])
        ClassAccessor classAccessor = new TableAccessor(table)
        for(tuple in data) {
            String property = tuple[0]
            def objects = tuple[1]
            for (object in objects) {
                addEvent(classAccessor, object, property, calendar);
            }
        }
        calendars.add(calendar)
    }

    protected def addEvent(TableAccessor classAccessor, object, String property, Calendar calendar) {
        def name = ShortNameUtils.getName(classAccessor, object)
        def eventStart = new DateTime(object[property], DateTimeZone.UTC)
        def evtInterval = new Interval(eventStart, eventStart.toDateMidnight().plusDays(1));
        def prettyProperty = Util.guessToWords(property);
        def event = new Event(
                calendar, object.id + "_" + property,
                name + " - " + prettyProperty,
                evtInterval, null, null);
        events.add(event)
    }

    @Override
    void loadObjects(DateTime instant, int maxEvents) {
        //TODO andrebbero ordinati per giorni....
        def allObjects = []
        for(cal in calDefs) {
            Calendar calendar = new Calendar(cal[1], cal[0], cal[3])
            calendars.add(calendar)
            Table table = DatabaseLogic.findTableByQualifiedName(model, cal[1])
            Session session = application.getSession(table.schema.databaseName)
            for(col in cal[2]) {
                Criteria criteria = session.createCriteria(table.actualEntityName)
                criteria.add(Restrictions.ge(col, instant.toDate()))
                criteria.setMaxResults(maxEvents)
                criteria.addOrder(Order.asc(col))
                def objects = criteria.list()
                if(!objects.isEmpty()) {
                    allObjects.add([col, calendar, table, objects])
                }
            }
        }
        for(int i = 0; i < maxEvents && !allObjects.isEmpty(); i++) {
            int index = (int) (i % allObjects.size())
            def ccto = allObjects[index]
            def col = ccto[0]
            def calendar = ccto[1]
            def table = ccto[2]
            def objects = ccto[3]
            if(objects.isEmpty()) {
                allObjects.remove(index)
                i--;
            } else {
                ClassAccessor classAccessor = new TableAccessor(table)
                addEvent(classAccessor, objects.remove(0), col, calendar)
            }
        }
    }

}