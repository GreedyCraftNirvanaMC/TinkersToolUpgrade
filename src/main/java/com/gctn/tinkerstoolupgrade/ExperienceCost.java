package com.gctn.tinkerstoolupgrade;

/** 与游戏注册表无关的经验计算；可直接验证极端配置及整型边界。 */
final class ExperienceCost {
    private ExperienceCost() {}

    static long required(int base, double multiplier, int level) {
        long cost = base;
        // 保留旧版每级乘算后向下取整的顺序。倍率至少为 2，最多约 63 次即饱和，
        // 因此 Integer.MAX_VALUE 等级也不会导致递归栈溢出或数十亿次循环。
        for (int current = 0; current < level; current++) {
            double next = cost * multiplier;
            if (next >= Long.MAX_VALUE) { return Long.MAX_VALUE; }
            cost = Math.max(1, (long) next);
        }
        return cost;
    }

    static long add(long xp, long amount) {
        return xp > Long.MAX_VALUE - amount ? Long.MAX_VALUE : xp + amount;
    }
}
