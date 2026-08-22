package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateDimensionDTO;
import org.hackathon.data.dto.UpdateDimensionDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.ReviewDimension;
import org.hackathon.data.vo.DimensionIdVO;
import org.hackathon.data.vo.DimensionVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.ReviewDimensionMapper;
import org.hackathon.mapper.ReviewScoreMapper;
import org.hackathon.security.Context;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 轮次评分维度配置。评委总分按权重归一到百分制：
 * 总分 = Σ(维度得分 / 维度满分 × 维度权重) / Σ维度权重 × 100。
 * 权重之和为 100 时即等于各维度按百分比加权求和。
 */
@Service
@RequiredArgsConstructor
public class ReviewDimensionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    //中间比值保留的精度，最终结果统一保留两位
    private static final int SCALE = 8;

    private final ReviewDimensionMapper dimensionMapper;
    private final ReviewScoreMapper scoreMapper;

    /**
     * 本轮评分维度列表
     */
    public List<DimensionVO> listDimensions(Context ctx) {
        return dimensionMapper.selectByPhase(ctx.phase().getPhaseId()).stream()
                .map(DimensionVO::of).toList();
    }

    /**
     * 新增评分维度
     * @param dto 维度名称、满分、权重
     * @param ctx 已由 @Require(EVENT_ADMIN) 校验
     * @return 新维度id
     */
    public DimensionIdVO createDimension(CreateDimensionDTO dto, Context ctx) {
        Integer phaseId = ctx.phase().getPhaseId();
        requireUnscored(phaseId);
        LocalDateTime now = LocalDateTime.now();
        ReviewDimension po = new ReviewDimension(null, phaseId, dto.getName().trim(),
                dto.getDescription(), dto.getMaxScore(), dto.getWeight(), dto.getSortNo(),
                1, now, now);
        try {
            dimensionMapper.insert(po);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DIMENSION_ALREADY_EXIST);
        }
        return new DimensionIdVO(po.getDimensionId());
    }

    /**
     * 修改评分维度，本轮一旦有评委打分即锁定
     */
    public void updateDimension(Integer dimensionId, UpdateDimensionDTO dto, Context ctx) {
        ReviewDimension po = mustFind(dimensionId, ctx);
        requireUnscored(po.getPhaseId());
        po.setName(dto.getName().trim());
        po.setDescription(dto.getDescription());
        po.setMaxScore(dto.getMaxScore());
        po.setWeight(dto.getWeight());
        po.setSortNo(dto.getSortNo());
        po.setVersion(dto.getVersion());
        po.setUpdateTime(LocalDateTime.now());
        try {
            if (dimensionMapper.updateById(po) == 0) {
                throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DIMENSION_ALREADY_EXIST);
        }
    }

    /**
     * 删除评分维度，本轮一旦有评委打分即锁定
     */
    public void deleteDimension(Integer dimensionId, Context ctx) {
        ReviewDimension po = mustFind(dimensionId, ctx);
        requireUnscored(po.getPhaseId());
        dimensionMapper.deleteById(dimensionId);
    }

    /**
     * 取本轮维度，未配置维度时评委无从打分
     */
    public List<ReviewDimension> requireDimensions(Integer phaseId) {
        List<ReviewDimension> dimensions = dimensionMapper.selectByPhase(phaseId);
        if (dimensions.isEmpty()) {
            throw new BusinessException(ResultCode.DIMENSION_UNSET);
        }
        return dimensions;
    }

    public List<ReviewDimension> listDimensions(Integer phaseId) {
        return dimensionMapper.selectByPhase(phaseId);
    }

    /**
     * 按权重把各维度得分归一为百分制总分
     * @param dimensions 本轮全部维度
     * @param scores 维度id → 得分，必须覆盖全部维度
     * @return 百分制总分，保留两位
     */
    public BigDecimal weightedTotal(List<ReviewDimension> dimensions, Map<Integer, BigDecimal> scores) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (ReviewDimension dimension : dimensions) {
            BigDecimal score = scores.get(dimension.getDimensionId());
            weighted = weighted.add(score
                    .divide(dimension.getMaxScore(), SCALE, RoundingMode.HALF_UP)
                    .multiply(dimension.getWeight()));
            weightSum = weightSum.add(dimension.getWeight());
        }
        return weighted.multiply(HUNDRED).divide(weightSum, 2, RoundingMode.HALF_UP);
    }

    private ReviewDimension mustFind(Integer dimensionId, Context ctx) {
        ReviewDimension po = dimensionMapper.selectById(dimensionId);
        //维度不属于路径中的轮次时按不存在处理，避免跨轮次越权改动
        if (po == null || !po.getPhaseId().equals(ctx.phase().getPhaseId())) {
            throw new BusinessException(ResultCode.DIMENSION_NOT_FOUND);
        }
        return po;
    }

    /**
     * 已有评委落下分数后，维度的增删改会让既有总分失去意义，故一律锁定
     */
    private void requireUnscored(Integer phaseId) {
        if (scoreMapper.existsByPhase(phaseId)) {
            throw new BusinessException(ResultCode.DIMENSION_LOCKED);
        }
    }
}
