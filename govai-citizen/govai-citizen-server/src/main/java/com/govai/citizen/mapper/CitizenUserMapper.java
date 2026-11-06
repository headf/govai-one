package com.govai.citizen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.citizen.entity.CitizenUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CitizenUserMapper extends BaseMapper<CitizenUser> {
}
