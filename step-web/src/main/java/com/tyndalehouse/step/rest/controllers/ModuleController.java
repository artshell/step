/*******************************************************************************
 * Copyright (c) 2012, Directors of the Tyndale STEP Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 *
 * Redistributions of source code must retain the above copyright 
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * Neither the name of the Tyndale House, Cambridge (www.TyndaleHouse.com)  
 * nor the names of its contributors may be used to endorse or promote 
 * products derived from this software without specific prior written 
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
 * THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.tyndalehouse.step.rest.controllers;

import com.google.inject.Inject;
import com.tyndalehouse.step.core.data.EntityDoc;
import com.tyndalehouse.step.core.exceptions.UserExceptionType;
import com.tyndalehouse.step.core.models.BibleInstaller;
import com.tyndalehouse.step.core.models.BibleVersion;
import com.tyndalehouse.step.core.models.VocabResponse;
import com.tyndalehouse.step.core.service.ModuleService;
import com.tyndalehouse.step.core.service.MorphologyService;
import com.tyndalehouse.step.core.service.SwingService;
import com.tyndalehouse.step.core.service.VocabularyService;
import com.tyndalehouse.step.models.info.Info;
import com.tyndalehouse.step.models.info.MorphInfo;
import com.tyndalehouse.step.models.info.VocabInfo;
import com.yammer.metrics.annotation.Timed;
import org.crosswire.jsword.book.BookCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tyndalehouse.step.core.exceptions.UserExceptionType.CONTROLLER_INITIALISATION_ERROR;
import static com.tyndalehouse.step.core.exceptions.UserExceptionType.USER_MISSING_FIELD;
import static com.tyndalehouse.step.core.utils.StringUtils.isNotBlank;
import static com.tyndalehouse.step.core.utils.StringUtils.split;
import static com.tyndalehouse.step.core.utils.ValidateUtils.notEmpty;
import static com.tyndalehouse.step.core.utils.ValidateUtils.notNull;

/**
 * The Module Controller servicing requests for module information
 */
public class ModuleController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleController.class);
    private final ModuleService moduleService;
    private final MorphologyService morphology;
    private final VocabularyService vocab;
    private final SwingService swingService;

    /**
     * sets up the controller to access module information
     *
     * @param moduleService the service allowing access to module information
     * @param morphology    the morphology service
     * @param vocabulary    the vocabulary service
     */
    @Inject
    public ModuleController(final ModuleService moduleService,
                            final MorphologyService morphology,
                            final VocabularyService vocabulary,
                            final SwingService swingService) {
        notNull(moduleService,
                "Intialising the module service in the module administration controller failed",
                CONTROLLER_INITIALISATION_ERROR);
        notNull(morphology,
                "Intialising the morphology service failed in the module administration controller",
                CONTROLLER_INITIALISATION_ERROR);
        notNull(swingService,
                "Intialising the swing service failed in the module administration controller",
                CONTROLLER_INITIALISATION_ERROR);
        this.swingService = swingService;
        this.moduleService = moduleService;
        this.morphology = morphology;
        this.vocab = vocabulary;
    }

    /**
     * a REST method that returns version of the Bible that are available
     *
     * @return all versions of modules that are considered to be Bibles.
     */
    public List<BibleVersion> getAllModules() {
        return this.moduleService.getAvailableModules();
    }

    /**
     * a REST method that returns version of the Bible that are not yet installed
     *
     * @param installerIndex the index of the installer to look up
     * @param types          a comma-delimited list of categories of modules to include
     * @return all versions of modules that are considered to be modules and usable by STEP.
     */
    public List<BibleVersion> getAllInstallableModules(final String installerIndex, final String types) {
        notNull(types, "No types of modules were provided", UserExceptionType.SERVICE_VALIDATION_ERROR);
        notNull(installerIndex, "No index to installer", UserExceptionType.SERVICE_VALIDATION_ERROR);
        final String[] values = split(types, ",");

        final BookCategory[] categories = new BookCategory[values.length];
        for (int i = 0; i < values.length; i++) {
            categories[i] = BookCategory.valueOf(values[i]);
        }

        return this.moduleService.getAllInstallableModules(Integer.parseInt(installerIndex), categories);
    }

    /**
     * Creates and returns a bible installer
     *
     * @return the bible installer that was created
     */
    public BibleInstaller addDirectoryInstaller() {
        return this.swingService.addDirectoryInstaller();
    }

    /**
     * a method that returns all the definitions for a particular key
     *
     * @param vocabIdentifiers the strong number
     * @param reference        the reference in which this can be found
     * @return the definition(s) that can be resolved from the reference provided
     */
    public Info getInfo(final String version, final String reference, final String vocabIdentifiers) {
        return this.getInfo(version, reference, vocabIdentifiers, null);
    }

    /**
     * a method that returns all the definitions for a particular key
     *
     * @param version          the version that holds the reference
     * @param reference        the reference in which this can be found
     * @param vocabIdentifiers the strong number
     * @param morphIdentifiers the morphology code to lookup
     * @return the definition(s) that can be resolved from the reference provided
     */
    @Timed(name = "full-vocab", group = "analysis", rateUnit = TimeUnit.SECONDS, durationUnit = TimeUnit.MILLISECONDS)
    public Info getInfo(
            final String version,
            final String reference,
            final String vocabIdentifiers,
            final String morphIdentifiers) {
        LOGGER.debug("Getting information for [{}], [{}], [{}]", new Object[]{reference, this.vocab, morphIdentifiers});

        final Info i = new Info();
        i.setMorphInfos(translateToInfo(this.morphology.getMorphology(morphIdentifiers), true));

        if (isNotBlank(vocabIdentifiers)) {
            i.setVocabInfos(translateToVocabInfo(this.vocab.getDefinitions(version, reference, vocabIdentifiers), true));
        }
        return i;
    }

    /**
     * a method that returns all the definitions for a particular key
     *
     *
     * @param version the version that holds the reference
     * @param reference        the reference in which this can be found
     * @param vocabIdentifiers the strong number
     * @return the definition(s) that can be resolved from the reference provided
     */
    @Timed(name = "quick-vocab", group = "analysis", rateUnit = TimeUnit.SECONDS, durationUnit = TimeUnit.MILLISECONDS)
    public Info getQuickInfo(final String version, final String reference, final String vocabIdentifiers) {
        return getQuickInfo(version, reference, vocabIdentifiers, null);
    }

    /**
     * a method that returns all the definitions for a particular key
     *
     * @param version the version that holds the reference
     * @param vocabIdentifiers the strong number
     * @param morphIdentifiers the morphology code to lookup
     * @return the definition(s) that can be resolved from the reference provided
     * @parma reference the verse in which the word is found
     */
    @Timed(name = "quick-vocab", group = "analysis", rateUnit = TimeUnit.SECONDS, durationUnit = TimeUnit.MILLISECONDS)
    public Info getQuickInfo(final String version, final String reference, final String vocabIdentifiers, final String morphIdentifiers) {
        // notEmpty(strong, "A reference must be provided to obtain a definition", USER_MISSING_FIELD);
        LOGGER.debug("Getting quick information for [{}], [{}]",
                new Object[]{this.vocab, morphIdentifiers});

        final Info i = new Info();
        i.setMorphInfos(translateToInfo(this.morphology.getQuickMorphology(morphIdentifiers), false));

        if (isNotBlank(vocabIdentifiers)) {
            i.setVocabInfos(translateToVocabInfo(this.vocab.getQuickDefinitions(version, reference, vocabIdentifiers), false));
        }
        return i;
    }

    /**
     * Copies over information.
     *
     * @param vocabResponse  the vocab response, including the definitions and the mappings to their related words
     * @param includeAllInfo true to include all information
     * @return a list of infos
     */
    private List<VocabInfo> translateToVocabInfo(final VocabResponse vocabResponse,
                                                 final boolean includeAllInfo) {
        final List<VocabInfo> morphologyInfos = new ArrayList<VocabInfo>(
                vocabResponse.getDefinitions().length);
        EntityDoc[] definitions = vocabResponse.getDefinitions();
        for (int i = 0; i < definitions.length; i++) {
            EntityDoc d = definitions[i];
            morphologyInfos.add(new VocabInfo(d, vocabResponse.getRelatedWords(), includeAllInfo));
        }
        return morphologyInfos;
    }

    /**
     * Morphology to information for the UI
     *
     * @param morphologies   the list of all morphologies
     * @param includeAllInfo true to include all information
     * @return the morphology information pojo
     */
    private List<MorphInfo> translateToInfo(final List<EntityDoc> morphologies, final boolean includeAllInfo) {
        final List<MorphInfo> morphologyInfos = new ArrayList<MorphInfo>(morphologies.size());
        for (final EntityDoc m : morphologies) {
            morphologyInfos.add(new MorphInfo(m, includeAllInfo));
        }
        return morphologyInfos;
    }
}
