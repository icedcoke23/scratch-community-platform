package com.scratch.community.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.system.entity.SystemConfig;
import com.scratch.community.module.system.mapper.SystemConfigMapper;
import com.scratch.community.module.system.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SystemConfigMapper configMapper;

    /**
     * 获取所有配置
     */
    @Transactional(readOnly = true)
    public List<ConfigVO> listAll() {
        List<SystemConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId));
        return configs.stream().map(this::toVO).toList();
    }

    /**
     * 根据 key 获取配置值
     */
    @Transactional(readOnly = true)
    public String getValue(String key) {
        SystemConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 更新配置
     */
    @Transactional
    public void updateConfig(String key, String value, String description) {
        SystemConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config == null) {
            throw new BizException(ErrorCode.CONFIG_NOT_FOUND);
        }
        config.setConfigValue(value);
        if (description != null && !description.isBlank()) {
            config.setDescription(description);
        }
        configMapper.updateById(config);
        log.info("更新配置: key={}, value={}", key, value);
    }

    private ConfigVO toVO(SystemConfig entity) {
        ConfigVO vo = new ConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
