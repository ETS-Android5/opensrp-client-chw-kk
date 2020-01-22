package org.smartregister.chw.dao;

import org.jetbrains.annotations.Nullable;
import org.smartregister.dao.AbstractDao;

import java.util.List;

public class ScheduleDao extends AbstractDao {

    //TODO
    public static @Nullable List<String> getActiveANCWomen(String scheduleName, String scheduleGroup) {
        String sql = "select base_entity_id from ec_anc_register where is_closed = 0 and base_entity_id not in " +
                "(select base_entity_id from schedule_service where schedule_name = '" + scheduleName + "' and schedule_group_name = '" + scheduleGroup + "')";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        return AbstractDao.readData(sql, dataMap);
    }

    public static @Nullable List<String> getActivePNCWomen(String scheduleName, String scheduleGroup) {
        String sql = "select base_entity_id from ec_pregnancy_outcome where is_closed = 0 and base_entity_id not in " +
                "(select base_entity_id from schedule_service where schedule_name = '" + scheduleName + "' and schedule_group_name = '" + scheduleGroup + "')";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        return AbstractDao.readData(sql, dataMap);
    }

    public static @Nullable List<String> getActiveChildren(String scheduleName, String scheduleGroup) {
        String sql = "select base_entity_id from ec_child where is_closed = 0 and base_entity_id not in " +
                "(select base_entity_id from schedule_service where schedule_name = '" + scheduleName + "' and schedule_group_name = '" + scheduleGroup + "')";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        return AbstractDao.readData(sql, dataMap);
    }

    public static @Nullable List<String> getActiveFamilies(String scheduleName, String scheduleGroup) {
        String sql = "select base_entity_id from ec_family where is_closed = 0 and base_entity_id not in " +
                "(select base_entity_id from schedule_service where schedule_name = '" + scheduleName + "' and schedule_group_name = '" + scheduleGroup + "')";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        return AbstractDao.readData(sql, dataMap);
    }

    public static @Nullable List<String> getActiveFPWomen(String scheduleName, String scheduleGroup) {
        String sql = "select base_entity_id from ec_family_planning where is_closed = 0 and base_entity_id not in " +
                "(select base_entity_id from schedule_service where schedule_name = '" + scheduleName + "' and schedule_group_name = '" + scheduleGroup + "')";

        DataMap<String> dataMap = c -> getCursorValue(c, "base_entity_id");
        return AbstractDao.readData(sql, dataMap);
    }
}
