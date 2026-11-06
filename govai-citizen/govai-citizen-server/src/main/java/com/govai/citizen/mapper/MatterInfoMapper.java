package com.govai.citizen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.citizen.entity.MatterInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MatterInfoMapper extends BaseMapper<MatterInfo> {

    List<MatterInfo> searchByKeyword(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("status") Integer status);
}
