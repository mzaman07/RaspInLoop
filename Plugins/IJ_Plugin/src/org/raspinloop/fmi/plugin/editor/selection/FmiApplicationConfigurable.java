/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raspinloop.fmi.plugin.editor.selection;

        import com.intellij.application.options.ModuleDescriptionsComboBox;
        import com.intellij.execution.ExecutionBundle;
        import com.intellij.execution.JavaExecutionUtil;
        import com.intellij.execution.ShortenCommandLine;
        import com.intellij.execution.application.ApplicationConfiguration;
        import com.intellij.execution.configurations.ConfigurationUtil;
        import com.intellij.execution.ui.*;
        import com.intellij.execution.util.JreVersionDetector;
        import com.intellij.openapi.options.ConfigurationException;
        import com.intellij.openapi.options.SettingsEditor;
        import com.intellij.openapi.project.Project;
        import com.intellij.openapi.ui.LabeledComponent;
        import com.intellij.psi.JavaCodeFragment;
        import com.intellij.psi.PsiClass;
        import com.intellij.psi.PsiElement;
        import com.intellij.psi.util.PsiMethodUtil;
        import com.intellij.ui.EditorTextFieldWithBrowseButton;
        import com.intellij.ui.PanelWithAnchor;
        import com.intellij.util.ui.UIUtil;
        import org.jetbrains.annotations.NotNull;
        import org.jetbrains.annotations.Nullable;
        import org.raspinloop.fmi.plugin.configuration.FmiRunnerConfiguration;

        import javax.swing.*;
        import java.awt.event.ActionEvent;
        import java.awt.event.ActionListener;

public class FmiApplicationConfigurable extends SettingsEditor<FmiRunnerConfiguration> implements PanelWithAnchor {
    private CommonJavaParametersPanel myCommonProgramParameters;
    private LabeledComponent<EditorTextFieldWithBrowseButton> myMainClass;
    private LabeledComponent<ModuleDescriptionsComboBox> myModule;
    private LabeledComponent<ShortenCommandLineModeCombo> myShortenClasspathModeCombo;
    private JPanel myWholePanel;

    private final ConfigurationModuleSelector myModuleSelector;
    private JrePathEditor myJrePathEditor;
    private JCheckBox myShowSwingInspectorCheckbox;
    private LabeledComponent myIncludeProvidedDeps;
    private LabeledComponent<HardwareConfigurationSelector> hardwareConfigurationSelector;
    private final JreVersionDetector myVersionDetector;
    private final Project myProject;
    private JComponent myAnchor;

    public FmiApplicationConfigurable(final Project project) {
        myProject = project;
        myModuleSelector = new ConfigurationModuleSelector(project, myModule.getComponent());
        myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromSourceRootsDependencies(myModule.getComponent(), getMainClassField()));
        myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
        myModule.getComponent().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
            }
        });
        ClassBrowser.createApplicationClassBrowser(project, myModuleSelector).setField(getMainClassField());
        myVersionDetector = new JreVersionDetector();

        myShortenClasspathModeCombo.setComponent(new ShortenCommandLineModeCombo(myProject, myJrePathEditor, myModule.getComponent()));
        myAnchor = UIUtil.mergeComponentsWithAnchor(myMainClass, myCommonProgramParameters, myJrePathEditor, myModule,
                myShortenClasspathModeCombo, hardwareConfigurationSelector);
    }

    public void applyEditorTo(@NotNull final FmiRunnerConfiguration configuration) throws ConfigurationException {
        myCommonProgramParameters.applyTo(configuration);
        myModuleSelector.applyTo(configuration);
        final String className = getMainClassField().getText();
        final PsiClass aClass = myModuleSelector.findClass(className);
        configuration.MAIN_CLASS_NAME = aClass != null ? JavaExecutionUtil.getRuntimeQualifiedName(aClass) : className;
        configuration.ALTERNATIVE_JRE_PATH = myJrePathEditor.getJrePathOrName();
        configuration.ALTERNATIVE_JRE_PATH_ENABLED = myJrePathEditor.isAlternativeJreSelected();
        configuration.ENABLE_SWING_INSPECTOR = (myVersionDetector.isJre50Configured(configuration) || myVersionDetector.isModuleJre50Configured(configuration)) && myShowSwingInspectorCheckbox.isSelected();
        configuration.setShortenCommandLine((ShortenCommandLine)myShortenClasspathModeCombo.getComponent().getSelectedItem());

        updateShowSwingInspector(configuration);
    }

    public void resetEditorFrom(@NotNull final FmiRunnerConfiguration configuration) {
        myCommonProgramParameters.reset(configuration);
        myModuleSelector.reset(configuration);
        getMainClassField().setText(configuration.MAIN_CLASS_NAME != null ? configuration.MAIN_CLASS_NAME.replaceAll("\\$", "\\.") : "");
        myJrePathEditor.setPathOrName(configuration.ALTERNATIVE_JRE_PATH, configuration.ALTERNATIVE_JRE_PATH_ENABLED);
        myShortenClasspathModeCombo.getComponent().setSelectedItem(configuration.getShortenCommandLine());

        updateShowSwingInspector(configuration);
    }

    private void updateShowSwingInspector(final ApplicationConfiguration configuration) {
        if (myVersionDetector.isJre50Configured(configuration) || myVersionDetector.isModuleJre50Configured(configuration)) {
            myShowSwingInspectorCheckbox.setEnabled(true);
            myShowSwingInspectorCheckbox.setSelected(configuration.ENABLE_SWING_INSPECTOR);
            myShowSwingInspectorCheckbox.setText(ExecutionBundle.message("show.swing.inspector"));
        }
        else {
            myShowSwingInspectorCheckbox.setEnabled(false);
            myShowSwingInspectorCheckbox.setSelected(false);
            myShowSwingInspectorCheckbox.setText(ExecutionBundle.message("show.swing.inspector.disabled"));
        }
    }

    public EditorTextFieldWithBrowseButton getMainClassField() {
        return myMainClass.getComponent();
    }

    public CommonJavaParametersPanel getCommonProgramParameters() {
        return myCommonProgramParameters;
    }

    @NotNull
    public JComponent createEditor() {
        return myWholePanel;
    }

    private void createUIComponents() {
        myMainClass = new LabeledComponent<>();
        myMainClass.setComponent(new EditorTextFieldWithBrowseButton(myProject, true, new JavaCodeFragment.VisibilityChecker() {
            @Override
            public Visibility isDeclarationVisible(PsiElement declaration, PsiElement place) {
                if (declaration instanceof PsiClass) {
                    final PsiClass aClass = (PsiClass)declaration;
                    if (ConfigurationUtil.MAIN_CLASS.value(aClass) && PsiMethodUtil.findMainMethod(aClass) != null || place.getParent() != null && myModuleSelector.findClass(((PsiClass)declaration).getQualifiedName()) != null) {
                        return Visibility.VISIBLE;
                    }
                }
                return Visibility.NOT_VISIBLE;
            }
        }));

        hardwareConfigurationSelector = new LabeledComponent<>();
        hardwareConfigurationSelector.setComponent(new HardwareConfigurationSelector(myProject));

        myShortenClasspathModeCombo = new LabeledComponent<>();
    }

    @Override
    public JComponent getAnchor() {
        return myAnchor;
    }

    @Override
    public void setAnchor(@Nullable JComponent anchor) {
        this.myAnchor = anchor;
        myMainClass.setAnchor(anchor);
        myCommonProgramParameters.setAnchor(anchor);
        myJrePathEditor.setAnchor(anchor);
        myModule.setAnchor(anchor);
        myShortenClasspathModeCombo.setAnchor(anchor);
        hardwareConfigurationSelector.setAnchor(anchor);
    }
}