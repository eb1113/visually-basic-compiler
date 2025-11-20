package edu.westminsteru.cmpt355.level3.util;

import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

/** A class containing utility methods for dealing with trees */
public class Tree {

    public static <T extends TreeNode<T>> void print(T tree, PrintWriter out) {
        print(tree, out, new LinkedList<>());
        out.flush();
    }

    public static <T extends TreeNode<T>> void print(T tree) {
        print(tree, new PrintWriter(System.out));
    }

    private static <T extends TreeNode<T>> void print(T tree, PrintWriter out, Deque<Boolean> levels) {
        var iterator = levels.iterator();
        while (iterator.hasNext()) {
            var level = iterator.next();
            if (iterator.hasNext())
                out.printf("%3s", level ? "│ " : "  ");
            else
                out.printf("%3s", level ? "├—" : "└—");
        }

        String s = " " + tree.toString();
        out.println(s);

        var subLevels = new LinkedList<>(levels);
        subLevels.add(true);

        var childIterator = tree.children().iterator();
        while (childIterator.hasNext()) {
            var child = childIterator.next();
            if (!childIterator.hasNext()) {
                subLevels.removeLast();
                subLevels.add(false);
            }

            print(child, out, subLevels);
        }
    }

    public static <N extends TreeNode<N>> void preorder(TreeNode<N> tree, Consumer<TreeNode<N>> f) {
        f.accept(tree);
        for (var child : tree.children())
            preorder(child, f);
    }

    public static <N extends TreeNode<N>> void postorder(TreeNode<N> tree, Consumer<TreeNode<N>> f) {
        for (var child : tree.children())
            postorder(child, f);
        f.accept(tree);
    }
}
