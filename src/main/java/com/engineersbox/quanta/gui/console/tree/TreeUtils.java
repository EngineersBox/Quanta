package com.engineersbox.quanta.gui.console.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.StringTokenizer;

public class TreeUtils {

    private TreeUtils() {
        // TODO: Refactor to wrap JTree and DefaultTreeModel with methods
        throw new IllegalStateException("Utility class");
    }

    public static TreePath getTreePath(final TreeModel treeModel,
                                       final String pathString,
                                       final String pathSeparator) {
        final StringTokenizer tokenizer = new StringTokenizer(pathString, pathSeparator);
        final int tokenCount = tokenizer.countTokens();
        int tokenNumber = 1;
        int tokenFoundCount = 0;
        final Object[] path = new Object[(tokenCount > 0) ? tokenCount : 1];
        if (tokenCount > 0) {
            path[0] = treeModel.getRoot();
            tokenizer.nextToken();
            Object currentElement = treeModel.getRoot();
            boolean appended = true;
            while (appended && (tokenNumber < tokenCount)) {
                final int childCount = treeModel.getChildCount(currentElement);
                final String pathToken = tokenizer.nextToken();
                boolean found = false;
                appended = false;
                for (int index = 0; (index < childCount) && !found; index++) {
                    final DefaultMutableTreeNode childElement = (DefaultMutableTreeNode) treeModel.getChild(currentElement, index);
                    final Object userObject = childElement.getUserObject();
                    if (!(userObject instanceof TreeNodeLabel nodeLabel)) {
                        return null;
                    }
                    found = nodeLabel.value().equals(pathToken);
                    if (found) {
                        path[tokenNumber] = childElement;
                        currentElement = childElement;
                        appended = true;
                        tokenFoundCount++;
                    }
                }
                tokenNumber++;
            }
        }
        return ((tokenCount > 0) && (tokenCount - 1 == tokenFoundCount)) ? new TreePath(path) : null;
    }
}
