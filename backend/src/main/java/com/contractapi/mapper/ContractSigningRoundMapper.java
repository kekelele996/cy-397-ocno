package com.contractapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contractapi.entity.ContractSigningRound;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ContractSigningRoundMapper extends BaseMapper<ContractSigningRound> {
  @Select("SELECT * FROM contract_signing_rounds WHERE id = #{id} FOR UPDATE")
  ContractSigningRound selectByIdForUpdate(@Param("id") Long id);
}
