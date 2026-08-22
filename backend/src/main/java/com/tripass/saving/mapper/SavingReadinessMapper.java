package com.tripass.saving.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SavingReadinessMapper {

    boolean existsRegisteredTravelGoal(@Param("userId") Long userId);

    boolean existsLinkedAccount(@Param("userId") Long userId);

    boolean existsLinkedCard(@Param("userId") Long userId);
}
