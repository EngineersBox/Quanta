package com.engineersbox.quanta.debug.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class VariableTree<T> extends DefaultTreeModel {

    private static final String ROOT_NODE_VALUE = "@quanta__INTERNAL_TREE_ROOT";
    private final Map<String, DefaultMutableTreeNode> leaves;
    private final String pathDelimiter;

    public VariableTree(final String pathDelimiter) {
        super(new DefaultMutableTreeNode(VariableTree.ROOT_NODE_VALUE));
        this.pathDelimiter = pathDelimiter;
        this.leaves = new HashMap<>();
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
        final DefaultMutableTreeNode newLeaf = new DefaultMutableTreeNode(value);
        current.add(newLeaf);
        this.leaves.put(path, newLeaf);
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

    @SuppressWarnings({"unchecked"})
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

    public Collection<DefaultMutableTreeNode> getLeafNodes() {
        return this.leaves.values();
    }

    public Collection<DefaultMutableTreeNode> traverseGetLeafNodes() {
        final List<DefaultMutableTreeNode> traversedLeaves = new ArrayList<>();
        getLeafNodesRecursive((DefaultMutableTreeNode) super.getRoot(), traversedLeaves);
        return traversedLeaves;
    }

    private void getLeafNodesRecursive(final DefaultMutableTreeNode parent, final List<DefaultMutableTreeNode> leaves) {
        final Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements()) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.isLeaf()) {
                leaves.add(node);
            } else {
                getLeafNodesRecursive(node, leaves);
            }
        }
    }

}
