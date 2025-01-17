// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package git4idea.index.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsApplicationSettings
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.EditorTabPreview
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ExpandableItemsHandler
import com.intellij.util.ui.UIUtil
import git4idea.index.actions.GitStageDiffAction
import javax.swing.JComponent

class GitStageEditorDiffPreview(diffProcessor: GitStageDiffPreview, private val tree: ChangesTree,
                                targetComponent: JComponent, activateMainUi: () -> Unit) :
  EditorTabPreview(diffProcessor) {

  private val stageDiffPreview: GitStageDiffPreview get() = diffProcessor as GitStageDiffPreview

  init {
    escapeHandler = Runnable {
      closePreview()
      activateMainUi()
    }
    installListeners(tree, false)
    installNextDiffActionOn(targetComponent)
    UIUtil.putClientProperty(tree, ExpandableItemsHandler.IGNORE_ITEM_SELECTION, true)
  }

  override fun updateAvailability(event: AnActionEvent) {
    GitStageDiffAction.updateAvailability(event)
  }

  override fun getCurrentName(): String {
    return stageDiffPreview
             .currentChangeName?.let { changeName -> VcsBundle.message("commit.editor.diff.preview.title", changeName) }
           ?: VcsBundle.message("commit.editor.diff.preview.empty.title")
  }

  override fun hasContent(): Boolean {
    return stageDiffPreview.currentChangeName != null
  }

  override fun skipPreviewUpdate(): Boolean {
    return super.skipPreviewUpdate() || tree != IdeFocusManager.getInstance(project).focusOwner
  }

  override fun isPreviewOnDoubleClickAllowed(): Boolean = VcsApplicationSettings.getInstance().SHOW_EDITOR_PREVIEW_ON_DOUBLE_CLICK
  override fun isPreviewOnEnterAllowed(): Boolean = VcsApplicationSettings.getInstance().SHOW_EDITOR_PREVIEW_ON_DOUBLE_CLICK
}
