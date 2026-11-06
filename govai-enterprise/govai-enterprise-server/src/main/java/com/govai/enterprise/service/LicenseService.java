package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.entity.EnterpriseLicense;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import com.govai.enterprise.mapper.EnterpriseLicenseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 企业证照服务(证照列表/到期提醒/电子证照预览文件生成)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final EnterpriseLicenseMapper licenseMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final FileStorageService fileStorage;

    /**
     * 按企业查全部证照,并为缺 file_url 的证照生成电子证照预览文件(证照预览/下载闭环)
     */
    public List<EnterpriseLicense> listByEnterprise(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new BusinessException(401, "当前账号未绑定企业");
        }
        List<EnterpriseLicense> list = licenseMapper.selectList(new LambdaQueryWrapper<EnterpriseLicense>()
                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                .orderByDesc(EnterpriseLicense::getId));
        ensureCertificateFiles(list);
        return list;
    }

    /**
     * 证照到期提醒:查询将在 days 天内到期(含已过期)的证照
     */
    public List<EnterpriseLicense> listExpiring(Long enterpriseId, int days) {
        if (enterpriseId == null) {
            throw new BusinessException(401, "当前账号未绑定企业");
        }
        LocalDate threshold = LocalDate.now().plusDays(days);
        return licenseMapper.selectList(new LambdaQueryWrapper<EnterpriseLicense>()
                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                .eq(EnterpriseLicense::getStatus, 1)
                .le(EnterpriseLicense::getExpiryDate, threshold)
                .orderByAsc(EnterpriseLicense::getExpiryDate));
    }

    /**
     * 新增证照(审批办结出证等场景调用)
     */
    public EnterpriseLicense addLicense(Long enterpriseId, String licenseType, String licenseNo,
                                        String licenseName, LocalDate issueDate, LocalDate expiryDate,
                                        String fileUrl) {
        EnterpriseLicense license = new EnterpriseLicense();
        license.setEnterpriseId(enterpriseId);
        license.setLicenseType(licenseType);
        license.setLicenseNo(licenseNo);
        license.setLicenseName(licenseName);
        license.setIssueDate(issueDate);
        license.setExpiryDate(expiryDate);
        license.setFileUrl(fileUrl);
        license.setStatus(1);
        licenseMapper.insert(license);
        return license;
    }

    // ============ 电子证照预览文件生成 ============

    private void ensureCertificateFiles(List<EnterpriseLicense> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return;
        }
        EnterpriseInfo info = enterpriseInfoMapper.selectById(licenses.get(0).getEnterpriseId());
        for (EnterpriseLicense l : licenses) {
            if (StrUtil.isBlank(l.getFileUrl())) {
                String svg = buildCertificateSvg(l, info);
                String url = fileStorage.storeText("certificate", l.getId() + ".svg", svg);
                l.setFileUrl(url);
                licenseMapper.updateById(l);
            }
        }
    }

    /**
     * 生成一张电子证照 SVG(营业执照/许可证通用样式,含红章)
     */
    private String buildCertificateSvg(EnterpriseLicense l, EnterpriseInfo info) {
        String enterpriseName = info == null ? "" : esc(info.getEnterpriseName());
        String legalPerson = info == null ? "" : esc(info.getLegalPerson());
        String registerAddress = info == null ? "" : esc(info.getRegisterAddress());
        String creditCode = info == null ? "" : esc(info.getCreditCode());
        String licenseName = esc(l.getLicenseName());
        String licenseNo = esc(l.getLicenseNo());
        String licenseType = esc(l.getLicenseType());
        String issueDate = l.getIssueDate() == null ? "" : l.getIssueDate().toString();
        String expiryDate = l.getExpiryDate() == null ? "长期" : l.getExpiryDate().toString();
        long expiryLong = l.getExpiryDate() == null ? 99991231L : Long.parseLong(l.getExpiryDate().toString().replace("-", ""));

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"420\" viewBox=\"0 0 600 420\">");
        sb.append("<rect width=\"600\" height=\"420\" fill=\"#fdfaf0\"/>");
        sb.append("<rect x=\"14\" y=\"14\" width=\"572\" height=\"392\" fill=\"none\" stroke=\"#b78a3b\" stroke-width=\"2\"/>");
        sb.append("<rect x=\"22\" y=\"22\" width=\"556\" height=\"376\" fill=\"none\" stroke=\"#d8b878\" stroke-width=\"1\"/>");
        // 标题
        sb.append("<text x=\"300\" y=\"70\" text-anchor=\"middle\" font-size=\"34\" font-weight=\"bold\" fill=\"#8a1f1f\" font-family=\"SimHei,STHeiti,sans-serif\">").append(licenseType).append("</text>");
        sb.append("<text x=\"300\" y=\"100\" text-anchor=\"middle\" font-size=\"14\" fill=\"#b78a3b\" font-family=\"SimSun,serif\">").append(licenseName).append("</text>");
        sb.append("<line x1=\"120\" y1=\"116\" x2=\"480\" y2=\"116\" stroke=\"#d8b878\" stroke-width=\"1\"/>");
        // 企业信息
        sb.append("<text x=\"80\" y=\"155\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">企业名称：").append(enterpriseName).append("</text>");
        sb.append("<text x=\"80\" y=\"188\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">法定代表人：").append(legalPerson).append("</text>");
        sb.append("<text x=\"80\" y=\"221\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">统一社会信用代码：").append(creditCode).append("</text>");
        sb.append("<text x=\"80\" y=\"254\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">住所：").append(registerAddress).append("</text>");
        // 证照信息
        sb.append("<text x=\"80\" y=\"290\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">证照编号：").append(licenseNo).append("</text>");
        sb.append("<text x=\"80\" y=\"323\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">发证日期：").append(issueDate).append("</text>");
        sb.append("<text x=\"80\" y=\"356\" font-size=\"15\" fill=\"#333\" font-family=\"SimSun,serif\">有效期至：").append(expiryDate).append("</text>");
        // 红章
        sb.append("<circle cx=\"470\" cy=\"300\" r=\"52\" fill=\"none\" stroke=\"#c0392b\" stroke-width=\"3\"/>");
        sb.append("<text x=\"470\" y=\"295\" text-anchor=\"middle\" font-size=\"15\" font-weight=\"bold\" fill=\"#c0392b\" font-family=\"SimHei,STHeiti,sans-serif\">电子证照</text>");
        sb.append("<text x=\"470\" y=\"318\" text-anchor=\"middle\" font-size=\"11\" fill=\"#c0392b\" font-family=\"SimSun,serif\">").append(String.valueOf(expiryLong)).append("</text>");
        sb.append("<text x=\"470\" y=\"333\" text-anchor=\"middle\" font-size=\"11\" fill=\"#c0392b\" font-family=\"SimSun,serif\">GovAI One</text>");
        // 底注
        sb.append("<text x=\"300\" y=\"392\" text-anchor=\"middle\" font-size=\"11\" fill=\"#999\" font-family=\"SimSun,serif\">本证照为电子证照,与纸质证照具有同等法律效力</text>");
        sb.append("</svg>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
