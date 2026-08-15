package com.tripass.mypage.mapper;

import com.tripass.mypage.domain.NotificationSetting; // Need to create this if it doesn't exist
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationSettingMapper {
    NotificationSetting getSettingByUserId(@Param("userId") Long userId);
    void insertDefaultSetting(@Param("userId") Long userId);
    void updateSetting(NotificationSetting setting);
}
