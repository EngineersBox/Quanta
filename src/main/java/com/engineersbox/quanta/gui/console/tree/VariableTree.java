package com.engineersbox.quanta.gui.console.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class VariableTree<T> extends DefaultTreeModel {

    private static final String ROOT_NODE_VALUE = "@__INTERNAL_TREE_ROOT__";
    private final String pathDelimiter;

    public VariableTree(final String pathDelimiter) {
        super(new DefaultMutableTreeNode(VariableTree.ROOT_NODE_VALUE));
        this.pathDelimiter = pathDelimiter;
    }

    private String formatPathString(final String path) {
        return VariableTree.ROOT_NODE_VALUE + this.pathDelimiter + path;
    }

    public void insert(final String path,
                       final T value) {
        final StringTokenizer tokenizer = new StringTokenizer(path, this.pathDelimiter);
        DefaultMutableTreeNode current = (DefaultMutableTreeNode) super.getRoot();
        while (tokenizer.hasMoreTokens()) {
            final String label = tokenizer.nextToken();
            final DefaultMutableTreeNode existing = findMatchingChild(current, label);
            if (existing != null) {
                current = existing;
                continue;
            }
            final TreeNodeLabel treeNodeLabel = new TreeNodeLabel(label);
            final DefaultMutableTreeNode next = new DefaultMutableTreeNode(treeNodeLabel);
            current.add(next);
            current = next;
        }
        current.add(new DefaultMutableTreeNode(value));
    }

    private DefaultMutableTreeNode findMatchingChild(final DefaultMutableTreeNode current,
                                                     final String label) {
        if (current == null || current.isLeaf()) {
            return null;
        }
        final Enumeration<TreeNode> children = current.children();
        while (children.hasMoreElements()) {
            final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            final Object userObject = child.getUserObject();
            if (userObject instanceof TreeNodeLabel nodeLabel && nodeLabel.value().equals(label)) {
                return child;
            }
        }
        return null;
    }

    public TreePath getPath(final String pathString) {
        final StringTokenizer tokenizer = new StringTokenizer(
                formatPathString(pathString),
                this.pathDelimiter
        );
        final int tokenCount = tokenizer.countTokens();
        int tokenNumber = 1;
        int tokenFoundCount = 0;
        final Object[] path = new Object[(tokenCount > 0) ? tokenCount : 1];
        if (tokenCount > 0) {
            path[0] = super.getRoot();
            tokenizer.nextToken();
            Object currentElement = super.getRoot();
            boolean appended = true;
            while (appended && (tokenNumber < tokenCount)) {
                final int childCount = super.getChildCount(currentElement);
                final String pathToken = tokenizer.nextToken();
                boolean found = false;
                appended = false;
                for (int index = 0; (index < childCount) && !found; index++) {
                    final DefaultMutableTreeNode childElement = (DefaultMutableTreeNode) super.getChild(currentElement, index);
                    final Object userObject = childElement.getUserObject();
                    if (!(userObject instanceof TreeNodeLabel nodeLabel)) {
                        continue;
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

    public T get(final String path) throws NoSuchElementException {
        final TreePath treePath = getPath(path);
        if (treePath == null) {
            throw new NoSuchElementException();
        }
        final DefaultMutableTreeNode selectedComponent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (selectedComponent.isLeaf()) {
            throw new NoSuchElementException();
        }
        final DefaultMutableTreeNode child = (DefaultMutableTreeNode) selectedComponent.getFirstChild();
        return (T) child.getUserObject();
    }

}
