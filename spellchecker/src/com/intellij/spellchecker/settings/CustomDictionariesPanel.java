// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.spellchecker.settings;


import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.dictionary.CustomDictionaryProvider;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import com.intellij.ui.*;
import com.intellij.ui.table.TableView;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.io.FileUtilRt.extensionEquals;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.util.containers.ContainerUtil.concat;
import static java.util.Arrays.asList;

public class CustomDictionariesPanel extends JPanel {
  private final SpellCheckerSettings mySettings;
  @NotNull private final SpellCheckerManager myManager;
  private final CustomDictionariesTableView myCustomDictionariesTableView;
  private final List<String> removedDictionaries = new ArrayList<>();
  private final List<String> defaultDictionaries;

  public CustomDictionariesPanel(@NotNull SpellCheckerSettings settings, @NotNull Project project, @NotNull SpellCheckerManager manager) {
    mySettings = settings;
    myManager = manager;
    defaultDictionaries = project.isDefault() ? new ArrayList<>() : asList(SpellCheckerBundle.message("app.dictionary"), SpellCheckerBundle
      .message("project.dictionary"));
    myCustomDictionariesTableView =
      new CustomDictionariesTableView(new ArrayList<>(settings.getCustomDictionariesPaths()), defaultDictionaries);
    final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myCustomDictionariesTableView)

      .setAddActionName(SpellCheckerBundle.message("add.custom.dictionaries"))
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          myCustomDictionariesTableView.stopEditing();
          doChooseFiles(project, files -> files.stream()
            .map(VirtualFile::getPath)
            .map(PathUtil::toSystemDependentName)
            .filter(path -> !myCustomDictionariesTableView.getItems().contains(path))
            .forEach(path -> myCustomDictionariesTableView.getListTableModel().addRow(path)));
        }
      })

      .setRemoveActionName(SpellCheckerBundle.message("remove.custom.dictionaries"))
      .setRemoveAction(button -> {
        removedDictionaries.addAll(myCustomDictionariesTableView.getSelectedObjects());
        TableUtil.removeSelectedItems(myCustomDictionariesTableView);
      })
      .setRemoveActionUpdater(e -> !ContainerUtil.exists(myCustomDictionariesTableView.getSelectedObjects(), defaultDictionaries::contains))

      .setEditActionName(SpellCheckerBundle.message("edit.custom.dictionary"))
      .setEditAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton anActionButton) {
          String selectedDictionary = myCustomDictionariesTableView.getSelectedObject();
          if (selectedDictionary == null) return;

          if (defaultDictionaries.contains(selectedDictionary)) {
            selectedDictionary = selectedDictionary.equals(SpellCheckerBundle.message("app.dictionary"))
                                 ? myManager.getAppDictionaryPath()
                                 : myManager.getProjectDictionaryPath();
          }
          manager.openDictionaryInEditor(selectedDictionary);
        }
      })

      .disableUpDownActions();
    myCustomDictionariesTableView.getEmptyText().setText((SpellCheckerBundle.message("no.custom.dictionaries")));
    this.setLayout(new BorderLayout());
    this.add(decorator.createPanel(), BorderLayout.CENTER);
  }

  private void doChooseFiles(@NotNull Project project, @NotNull Consumer<? super List<VirtualFile>> consumer) {
    final FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, true) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return extensionEquals(file.getPath(), "dic");
      }
    };

    FileChooser.chooseFiles(fileChooserDescriptor, project, this.getParent(), project.getBaseDir(), consumer);
  }

  public List<String> getRemovedDictionaries() {
    return removedDictionaries;
  }

  public boolean isModified() {
    final List<String> oldPaths = mySettings.getCustomDictionariesPaths();
    final List<String> newPaths = ContainerUtil.filter(myCustomDictionariesTableView.getItems(), o -> !defaultDictionaries.contains(o));
    if (oldPaths.size() != newPaths.size()) {
      return true;
    }
    if (!newPaths.containsAll(oldPaths) || !oldPaths.containsAll(newPaths)) {
      return true;
    }
    return false;
  }

  public void reset() {
    myCustomDictionariesTableView.getListTableModel()
      .setItems(new ArrayList<>(concat(defaultDictionaries, mySettings.getCustomDictionariesPaths())));
    removedDictionaries.clear();
  }

  public void apply() {
    final List<String> oldPaths = mySettings.getCustomDictionariesPaths();
    final List<String> newPaths = new ArrayList<>(ContainerUtil.filter(myCustomDictionariesTableView.getItems(),
                                                                       dict -> !defaultDictionaries.contains(dict)));
    mySettings.setCustomDictionariesPaths(newPaths);
    myManager.updateBundledDictionaries(ContainerUtil.filter(oldPaths, o -> !newPaths.contains(o)));
  }

  public List<String> getValues() {
    return myCustomDictionariesTableView.getItems();
  }

  private static final class CustomDictionariesTableView extends TableView<String> {

    final TableCellRenderer myTypeRenderer;

    private CustomDictionariesTableView(@NotNull List<String> dictionaries,
                                        @NotNull List<String> defaultDictionaries) {
      myTypeRenderer = createTypeRenderer(defaultDictionaries);
      setModelAndUpdateColumns(new ListTableModel<>(createDictionaryColumnInfos(), concat(defaultDictionaries, dictionaries), 0));
      setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
      setShowGrid(false);
      setShowVerticalLines(false);
      setGridColor(getForeground());
      setTableHeader(null);
    }

    private static TableCellRenderer createTypeRenderer(List<String> defaultDictionaries) {
      return new TableCellRenderer() {
        final SimpleColoredComponent myLabel = new SimpleColoredComponent();

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {

          myLabel.clear();
          myLabel.append((String)value, SimpleTextAttributes.REGULAR_ATTRIBUTES);
          final String type;
          if (defaultDictionaries.contains(value)) {
            type = SpellCheckerBundle.message("built.in.dictionary");
          }
          else {
            final CustomDictionaryProvider provider = CustomDictionaryProvider.EP_NAME.getExtensionList().stream()
              .filter(dictionaryProvider -> dictionaryProvider.isApplicable((String)value))
              .findAny()
              .orElse(null);
            type = provider != null ? provider.getDictionaryType() : SpellCheckerBundle.message("words.list.dictionary");
          }
          myLabel.append(" [" + type + "]", GRAY_ATTRIBUTES);
          myLabel.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
          myLabel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
          return myLabel;
        }
      };
    }

    private ColumnInfo[] createDictionaryColumnInfos() {
      return new ColumnInfo[]{
        new ColumnInfo<String, String>(SpellCheckerBundle.message("custom.dictionary.title")) {
          @Override
          public String valueOf(final String info) {
            return info;
          }

          @Nullable
          @Override
          public TableCellRenderer getRenderer(String s) {
            return myTypeRenderer;
          }
        }
      };
    }
  }
}