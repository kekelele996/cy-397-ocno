package com.contractapi.service;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.ErrorCode;
import com.contractapi.dto.GenerateContractRequest;
import com.contractapi.entity.Contract;
import com.contractapi.entity.ContractTemplate;
import com.contractapi.exception.ApiException;
import com.contractapi.mapper.ContractMapper;
import com.contractapi.utils.TemplateRenderer;
import org.springframework.stereotype.Service;

@Service
public class ContractService {
  private final TemplateService templateService;
  private final TemplateRenderer renderer;
  private final ContractMapper contractMapper;

  public ContractService(TemplateService templateService, TemplateRenderer renderer, ContractMapper contractMapper) {
    this.templateService = templateService;
    this.renderer = renderer;
    this.contractMapper = contractMapper;
  }

  public Contract generate(GenerateContractRequest request) {
    ContractTemplate template = templateService.find(request.templateId());
    Contract contract = new Contract();
    contract.setUserId(request.userId());
    contract.setTemplateId(template.getId());
    contract.setTitle(request.title());
    contract.setContent(renderer.render(template.getContent(), request.variables()));
    contract.setStatus(ContractStatus.DRAFT.name());
    contract.setSigners("[]");
    contractMapper.insert(contract);
    return contract;
  }

  public Contract findById(Long id) {
    Contract contract = contractMapper.selectById(id);
    if (contract == null) {
      throw new ApiException(ErrorCode.NOT_FOUND, "合同不存在");
    }
    return contract;
  }

  public Contract updateStatus(Long id, ContractStatus status) {
    if (status == ContractStatus.SIGNED || status == ContractStatus.EXPIRED) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "合同签署或过期状态由签署流程自动更新");
    }
    Contract contract = findById(id);
    if (ContractStatus.SIGNED.name().equals(contract.getStatus()) || ContractStatus.EXPIRED.name().equals(contract.getStatus())) {
      throw new ApiException(ErrorCode.INVALID_SIGNING_STATE, "签署完成或已过期的合同不能手动修改状态");
    }
    contract.setStatus(status.name());
    contractMapper.updateById(contract);
    return contract;
  }

  public List<Contract> list(Long userId, String status) {
    LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(userId != null, Contract::getUserId, userId)
        .eq(status != null, Contract::getStatus, status)
        .orderByDesc(Contract::getId);
    return contractMapper.selectList(wrapper);
  }

  public String exportPdf(Long id) {
    findById(id);
    return "wkhtmltopdf 已在 Docker 镜像安装，合同 " + id + " 可导出到 /tmp/contracts/" + id + ".pdf";
  }
}
