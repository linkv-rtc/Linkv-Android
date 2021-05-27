package com.linkv.live;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by zhangsong on 20-5-19
 */
public class Main {
    private static Map<Character, Character> map = new HashMap<Character, Character>() {
        {
            put('[', ']');
            put('(', ')');
            put('{', '}');
        }
    };
    private static Stack<Character> stack = new Stack<>();
    /**
     *
     */
    private static List<Integer> list = new ArrayList<>();

    public static void main(String... args) {
//        System.out.println("huiwen: " + huiwen(1231));
//        System.out.println("index :" + indexOf(new int[]{1, 2, 3, 4, 5}, 0));
//        System.out.println("valid :" + valid("((())"));
//        System.out.println("valid :" + indexOf("abcac", "ac"));

//        Node head = new Node(1);
////        head.next = new Node(2);
//        Node rev = reverse(head);
//        while (rev != null) {
//            System.out.println("value: " + rev.value);
//            rev = rev.next;
//        }

//        int[] arr = new int[]{6, 3, 2, 5, 4};
//        sort(arr, 0, arr.length - 1);
//        System.out.println("sort: " + Arrays.toString(arr));

//        int[] arr = new int[]{6, 3, 2, 5, 4};
//        int[] result = indexOfSum(arr, 8);
//        System.out.println("indexOfSum " + 8 + " is at [" + result[0] + ", " + result[1] + "]");

//        int sum = 0;
//
//        int[] arr = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
//        ExpFilter filter = new ExpFilter(0.5f);
//        for (int item : arr) {
//            filter.apply(item);
//            sum += item;
//        }
//
//        System.out.println("filter sum: " + filter.filtered() + ", average: " + (sum / arr.length));

//        System.out.println("valid: " + valid("())"));
        int[][] matrix = new int[][]{{1, 2}, {3, 4}};
        rotate(matrix);

        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.println("result[" + i + "][" + j + "] = " + matrix[i][j]);
            }
        }
    }

    private static boolean huiwen(int x) {
        if (x < 0 || (x != 0 && x % 10 == 0)) {
            return false;
        }

        int reverse = 0;
        while (x > reverse) {
            reverse = reverse * 10 + x % 10;
            x /= 10;
        }

        return reverse == x || reverse / 10 == x;
    }

    private static int indexOf(int[] arr, int target) {
        int start = 0;
        int end = arr.length - 1;
        while (start <= end) {
            final int middle = (start + end) >>> 1;
            if (arr[middle] == target) {
                return middle;
            } else if (arr[middle] > target) {
                end = middle - 1;
            } else {
                start = middle + 1;
            }
        }
        return ~start;
    }

    private static boolean valid(String s) {
        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if (map.containsKey(c)) {
                stack.push(c);
            } else {
                Character top = stack.empty() ? '0' : stack.pop();
                if (c != map.get(top)) {
                    return false;
                }
            }
        }
        return stack.empty();
    }

    private static int indexOf(String src, String target) {
        int srcLength = src.length();
        int targetLength = target.length();

        if (targetLength > srcLength) {
            return -1;
        }

        char first = target.charAt(0);
        for (int i = 0; i <= srcLength - targetLength; i++) {
            if (first != src.charAt(i)) {
                continue;
            }

            int j = i + 1;
            for (int k = 1; k < targetLength; k++, j++) {
                if (src.charAt(j) != target.charAt(k)) {
                    break;
                }
            }

            if (j == i + targetLength) {
                return i;
            }
        }

        return -1;
    }

    private static int indexOf1(String src, String target) {
        final int srcLen = src.length();
        final int targetLen = target.length();
        if (srcLen > targetLen) {
            return -1;
        }

        char first = target.charAt(0);
        for (int i = 0; i <= srcLen - targetLen; i++) {
            if (src.charAt(i) != first) {
                continue;
            }

            int j = i + 1;
            for (int k = 1; k < targetLen; k++, j++) {
                if (src.charAt(j) != target.charAt(k)) {
                    break;
                }
            }

            if (j == i + targetLen) {
                return i;
            }
        }

        return -1;
    }

    private static Node reverse(Node head) {
        if (head == null || head.next == null) {
            return head;
        }

        Node temp = reverse(head.next);
        head.next.next = head;
        head.next = null;
        return temp;

//        Node prev = null;
//        Node cur = head;
//        while (cur != null) {
//            Node next = cur.next;
//            cur.next = prev;
//            prev = cur;
//            cur = next;
//        }
//
//        return prev;
    }

    private static Node reverseK(Node head, int k) {
        Node newHead = new Node(0);
        newHead.next = head;

        Node prev = newHead;
        Node end = newHead;

        while (end.next != null) {
            for (int i = 0; i < k && end != null; i++) {
                end = end.next;
            }
            if (end == null) {
                break;
            }
            Node start = prev.next;
            Node next = end.next;
            end.next = null;
            prev.next = reverse(start);
            start.next = next;
            prev = start;
            end = start;
        }

        return newHead.next;
    }

    /**
     * quick sort
     */
    private static void sort(int[] arr, int start, int end) {
        if (start >= end) {
            return;
        }

        int index = partition(arr, start, end);
        sort(arr, 0, index - 1);
        sort(arr, index + 1, end);
    }

    private static int partition(int[] arr, int start, int end) {
        int pivot = arr[start];

        while (start < end) {
            while (start < end && arr[end] > pivot) end--;
            arr[start] = arr[end];
            while (start < end && arr[start] < pivot) start++;
            arr[end] = arr[start];
            arr[start] = pivot;
        }

        return start;
    }

    private static int[] indexOfSum(int[] arr, int target) {
        for (int i = 0; i < arr.length - 1; i++) {
            final int item = arr[i];
            final int minus = target - item;
            if (list.contains(minus)) {
                return new int[]{list.indexOf(minus), i};
            } else {
                list.add(item);
            }
        }
        return new int[]{-1, -1};
    }

    private static void rotate(int[][] matrix) {
        int n = matrix.length;

        int[][] result = new int[n][n];
        for (int l = 0; l < n; l++) {
            for (int r = n - 1; r > -1; r--) {
                result[l][n - r - 1] = matrix[r][l];
            }
        }

        for (int i = 0; i < n; i++) {
            System.arraycopy(result[i], 0, matrix[i], 0, n);
        }
    }

    // [3,2] 3
    // [2,3] 3
    private static int remove(int[] array, int val) {
        int start = 0;
        int end = array.length - 1;

        while (start < end) {
            if (array[start] == val) {
                array[start] = array[end];
                end--;
            } else {
                start++;
            }
        }

        return 0;
    }

    private static class Node {
        Node next;
        int value;

        public Node(int value) {
            this.value = value;
        }
    }

    private static class ExpFilter {
        private float alpha;
        private float filtered;

        public ExpFilter(float alpha) {
            this.alpha = alpha;
            this.filtered = -1;
        }

        public void apply(float sample) {
            if (filtered == -1) {
                filtered = sample;
            } else {
                filtered = alpha * filtered + (1 - alpha) * sample;
            }
        }

        public float filtered() {
            return filtered;
        }
    }
}
