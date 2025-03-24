package kakao.login.service;

import kakao.login.entity.SectionSettings;
import kakao.login.repository.SectionSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectionSettingsService {

    @Autowired
    private SectionSettingsRepository sectionSettingsRepository;

    public Boolean getUseSectionsStatus() {
        SectionSettings settings = sectionSettingsRepository.findFirstByOrderById();
        // 설정이 없으면 기본값 true 반환
        return settings != null ? settings.getUseSections() : true;
    }

    @Transactional
    public SectionSettings updateUseSectionsStatus(Boolean useSections) {
        SectionSettings settings = sectionSettingsRepository.findFirstByOrderById();

        if (settings == null) {
            // 설정이 없으면 새로 생성
            settings = new SectionSettings();
            settings.setUseSections(useSections);
        } else {
            // 설정이 있으면 업데이트
            settings.setUseSections(useSections);
        }

        return sectionSettingsRepository.save(settings);
    }
}