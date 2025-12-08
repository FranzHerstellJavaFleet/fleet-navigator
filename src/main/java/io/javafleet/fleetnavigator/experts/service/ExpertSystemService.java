package io.javafleet.fleetnavigator.experts.service;

import io.javafleet.fleetnavigator.experts.dto.*;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.experts.repository.ExpertModeRepository;
import io.javafleet.fleetnavigator.experts.repository.ExpertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service für das Experten-System.
 * Verwaltet Experten und deren Modi, führt Anfragen mit Auto-Mode-Detection aus.
 */
@Service
@Slf4j
public class ExpertSystemService {

    // ==================== CONSTANTS ====================

    private static final String DEFAULT_MODEL = "llama3.1:8b-instruct-q8_0";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final double DEFAULT_TOP_P = 0.9;
    private static final int DEFAULT_NUM_CTX = 8192;
    private static final int DEFAULT_PRIORITY = 0;

    // ==================== DEPENDENCIES ====================

    private final ExpertRepository expertRepository;
    private final ExpertModeRepository modeRepository;

    public ExpertSystemService(ExpertRepository expertRepository,
                               ExpertModeRepository modeRepository) {
        this.expertRepository = expertRepository;
        this.modeRepository = modeRepository;
    }

    // ==================== EXPERT CRUD ====================

    @Transactional(readOnly = true)
    public List<Expert> getAllExperts() {
        return expertRepository.findAllActiveOrderByName();
    }

    @Transactional(readOnly = true)
    public Optional<Expert> getExpertById(Long id) {
        return expertRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Expert> getExpertByName(String name) {
        return expertRepository.findByName(name);
    }

    @Transactional
    public Expert createExpert(CreateExpertRequest request) {
        validateExpertNameUnique(request.getName());

        Expert expert = mapToNewExpert(request);
        Expert saved = expertRepository.save(expert);

        saved = addInitialModes(saved, request.getModes());

        log.info("Experte erstellt: {} ({})", saved.getName(), saved.getRole());

        return saved;
    }

    @Transactional
    public Expert updateExpert(Long id, CreateExpertRequest request) {
        Expert expert = findExpertOrThrow(id);

        updateExpertFromRequest(expert, request);
        Expert saved = expertRepository.save(expert);

        log.info("Experte aktualisiert: {} ({})", saved.getName(), saved.getRole());

        return saved;
    }

    @Transactional
    public void deleteExpert(Long id) {
        Expert expert = findExpertOrThrow(id);

        expertRepository.delete(expert);

        log.info("Experte gelöscht: {}", expert.getName());
    }

    // ==================== MODE CRUD ====================

    @Transactional(readOnly = true)
    public List<ExpertMode> getModesForExpert(Long expertId) {
        return modeRepository.findActiveByExpertIdOrderByPriority(expertId);
    }

    @Transactional
    public ExpertMode addModeToExpert(Long expertId, CreateExpertModeRequest request) {
        Expert expert = findExpertOrThrow(expertId);
        validateModeNameUnique(expertId, request.getName());

        ExpertMode mode = mapToNewMode(expert, request);
        ExpertMode saved = modeRepository.save(mode);

        log.info("Modus '{}' zu Experte '{}' hinzugefügt", saved.getName(), expert.getName());
        return saved;
    }

    @Transactional
    public ExpertMode updateMode(Long modeId, CreateExpertModeRequest request) {
        ExpertMode mode = findModeOrThrow(modeId);

        updateModeFromRequest(mode, request);
        ExpertMode saved = modeRepository.save(mode);

        log.info("Modus '{}' aktualisiert", saved.getName());
        return saved;
    }

    @Transactional
    public void deleteMode(Long modeId) {
        ExpertMode mode = findModeOrThrow(modeId);
        modeRepository.delete(mode);
        log.info("Modus '{}' gelöscht", mode.getName());
    }

    // ==================== HELPER METHODS - ENTITY LOOKUP ====================

    private Expert findExpertOrThrow(Long id) {
        return expertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Experte nicht gefunden: " + id));
    }

    private ExpertMode findModeOrThrow(Long id) {
        return modeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modus nicht gefunden: " + id));
    }

    // ==================== HELPER METHODS - VALIDATION ====================

    private void validateExpertNameUnique(String name) {
        if (expertRepository.existsByName(name)) {
            throw new IllegalArgumentException("Experte mit Name '" + name + "' existiert bereits");
        }
    }

    private void validateModeNameUnique(Long expertId, String modeName) {
        if (modeRepository.existsByExpert_IdAndName(expertId, modeName)) {
            throw new IllegalArgumentException("Modus '" + modeName + "' existiert bereits für diesen Experten");
        }
    }

    // ==================== HELPER METHODS - MAPPING ====================

    private Expert mapToNewExpert(CreateExpertRequest request) {
        Expert expert = new Expert();
        expert.setName(request.getName());
        expert.setRole(request.getRole());
        expert.setDescription(request.getDescription());
        expert.setBasePrompt(request.getBasePrompt());
        expert.setPersonalityPrompt(request.getPersonalityPrompt());
        expert.setAvatarUrl(request.getAvatarUrl());
        expert.setBaseModel(getOrDefault(request.getBaseModel(), DEFAULT_MODEL));
        expert.setDefaultTemperature(getOrDefault(request.getDefaultTemperature(), DEFAULT_TEMPERATURE));
        expert.setDefaultTopP(getOrDefault(request.getDefaultTopP(), DEFAULT_TOP_P));
        expert.setDefaultNumCtx(getOrDefault(request.getDefaultNumCtx(), DEFAULT_NUM_CTX));
        expert.setDefaultMaxTokens(request.getDefaultMaxTokens());
        expert.setAutoWebSearch(request.getAutoWebSearch());
        expert.setSearchDomains(request.getSearchDomains());
        expert.setMaxSearchResults(request.getMaxSearchResults());
        expert.setAutoFileSearch(request.getAutoFileSearch());
        // Standard-Dokumentverzeichnis: Expertenname wenn nicht angegeben
        String docDir = request.getDocumentDirectory();
        if (docDir == null || docDir.isBlank()) {
            docDir = request.getName();  // z.B. "Roland" → ~/Dokumente/Fleet-Navigator/Roland/
        }
        expert.setDocumentDirectory(docDir);
        expert.setPreferredMateId(request.getPreferredMateId());
        expert.setActive(true);
        return expert;
    }

    private void updateExpertFromRequest(Expert expert, CreateExpertRequest request) {
        setIfNotNull(request.getName(), expert::setName);
        setIfNotNull(request.getRole(), expert::setRole);
        setIfNotNull(request.getDescription(), expert::setDescription);
        setIfNotNull(request.getBasePrompt(), expert::setBasePrompt);
        setIfNotNull(request.getPersonalityPrompt(), expert::setPersonalityPrompt);
        setIfNotNull(request.getAvatarUrl(), expert::setAvatarUrl);
        setIfNotNull(request.getBaseModel(), expert::setBaseModel);
        setIfNotNull(request.getDefaultTemperature(), expert::setDefaultTemperature);
        setIfNotNull(request.getDefaultTopP(), expert::setDefaultTopP);
        setIfNotNull(request.getDefaultNumCtx(), expert::setDefaultNumCtx);
        setIfNotNull(request.getDefaultMaxTokens(), expert::setDefaultMaxTokens);
        setIfNotNull(request.getAutoWebSearch(), expert::setAutoWebSearch);
        setIfNotNull(request.getSearchDomains(), expert::setSearchDomains);
        setIfNotNull(request.getMaxSearchResults(), expert::setMaxSearchResults);
        setIfNotNull(request.getAutoFileSearch(), expert::setAutoFileSearch);
        setIfNotNull(request.getDocumentDirectory(), expert::setDocumentDirectory);
        setIfNotNull(request.getPreferredMateId(), expert::setPreferredMateId);
    }

    private ExpertMode mapToNewMode(Expert expert, CreateExpertModeRequest request) {
        ExpertMode mode = new ExpertMode();
        mode.setExpert(expert);
        mode.setName(request.getName());
        mode.setDescription(request.getDescription());
        mode.setPromptAddition(request.getPromptAddition());
        mode.setKeywords(request.getKeywords());
        mode.setTemperature(request.getTemperature());
        mode.setTopP(request.getTopP());
        mode.setTopK(request.getTopK());
        mode.setRepeatPenalty(request.getRepeatPenalty());
        mode.setNumCtx(request.getNumCtx());
        mode.setMaxTokens(request.getMaxTokens());
        mode.setPriority(getOrDefault(request.getPriority(), DEFAULT_PRIORITY));
        mode.setActive(true);
        return mode;
    }

    private void updateModeFromRequest(ExpertMode mode, CreateExpertModeRequest request) {
        setIfNotNull(request.getName(), mode::setName);
        setIfNotNull(request.getDescription(), mode::setDescription);
        setIfNotNull(request.getPromptAddition(), mode::setPromptAddition);
        setIfNotNull(request.getKeywords(), mode::setKeywords);
        setIfNotNull(request.getTemperature(), mode::setTemperature);
        setIfNotNull(request.getTopP(), mode::setTopP);
        setIfNotNull(request.getTopK(), mode::setTopK);
        setIfNotNull(request.getRepeatPenalty(), mode::setRepeatPenalty);
        setIfNotNull(request.getNumCtx(), mode::setNumCtx);
        setIfNotNull(request.getMaxTokens(), mode::setMaxTokens);
        setIfNotNull(request.getPriority(), mode::setPriority);
    }

    private Expert addInitialModes(Expert expert, List<CreateExpertModeRequest> modes) {
        if (modes == null || modes.isEmpty()) {
            return expert;
        }

        for (CreateExpertModeRequest modeReq : modes) {
            addModeToExpert(expert.getId(), modeReq);
        }

        return expertRepository.findById(expert.getId()).orElse(expert);
    }

    // ==================== UTILITY METHODS ====================

    private <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private <T> void setIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

}
