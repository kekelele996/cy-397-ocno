package com.contractapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contractapi.entity.Contract;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ContractMapper extends BaseMapper<Contract> {
  @Select("SELECT * FROM contracts WHERE id = #{id} FOR UPDATE")
  Contract selectByIdForUpdate(@Param("id") Long id);
}
