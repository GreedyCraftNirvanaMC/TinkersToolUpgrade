package com.gctn.tinkerstoolupgrade;

/** 无额外测试库；覆盖手动游玩难以触及的成本、逐级舍入和溢出边界。 */
public final class ExperienceCostChecks {
    private ExperienceCostChecks() {}
    public static void main(String[] args) {
        check(ExperienceCost.required(500, 2, 0) == 500, "first upgrade");
        check(ExperienceCost.required(500, 2, 1) == 1000, "second upgrade");
        check(ExperienceCost.required(4500, 2, 3) == 36000, "area tool cost");
        check(ExperienceCost.required(3, 2.5, 3) == 42, "round each level: 3, 7, 17, 42");
        check(ExperienceCost.required(1, 2, 62) == 1L << 62, "largest exact power of two");
        check(ExperienceCost.required(1, 2, 63) == Long.MAX_VALUE, "cost saturation");
        check(ExperienceCost.required(Integer.MAX_VALUE, 100, Integer.MAX_VALUE) == Long.MAX_VALUE, "extreme level terminates");
        check(ExperienceCost.add(499, 1) == 500, "exact threshold");
        check(ExperienceCost.add(Long.MAX_VALUE - 5, 5) == Long.MAX_VALUE, "exact XP limit");
        check(ExperienceCost.add(Long.MAX_VALUE - 5, 6) == Long.MAX_VALUE, "XP overflow saturation");
        System.out.println("Passed 10 experience boundary checks.");
    }
    private static void check(boolean condition, String name) {
        if (!condition) { throw new AssertionError(name); }
    }
}
