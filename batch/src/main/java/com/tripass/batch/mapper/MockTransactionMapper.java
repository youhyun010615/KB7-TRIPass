package com.tripass.batch.mapper;

import com.tripass.batch.dto.TransactionMockDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface MockTransactionMapper {
    List<Long> selectActiveAccountIds();
    int insertMockTransaction(TransactionMockDto dto);
}