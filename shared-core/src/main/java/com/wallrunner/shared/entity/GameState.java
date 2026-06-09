package com.wallrunner.shared.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单局游戏的完整运行时状态（已修正）。
 *
 * 修正内容：
 * 1. players 使用 ConcurrentHashMap（已有）
 * 2. obstacles、collectibles、activeEffects 使用 CopyOnWriteArrayList（线程安全）
 * 3. 更新注释，说明线程安全由集合本身保证
 *
 * 设计原则：
 * - 内部集合均为线程安全实现，RoomManager 可直接共享 GameState 实例。
 * - Server 端多线程并发访问安全，Client 端单线程 JavaFX 亦可正常使用。
 * - 预留可收集物、全局特效、难度系统等扩展空间。
 */
public class GameState {

    /* ============================================================
       核心游戏状态
       ============================================================ */
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private List<Obstacle> obstacles = new CopyOnWriteArrayList<>();
    private String phase = "menu";      // "menu" | "playing" | "gameover"
    private int frames = 0;

    /* ============================================================
       摄像机（全局显示摄像机，仅用于渲染与障碍物生成）
       ============================================================ */
    private double cameraY = 0;
    private double cameraTargetY = 0;
    private double nextSpawnCameraY = 0;

    /* ============================================================
       时间奖励系统
       ============================================================ */
    private double timeBonusInterval = 5.0;
    private int timeBonusPoints = 10;
    private double timeBonusAccumulator = 0.0;

    /* ============================================================
       预留扩展区 —— 可收集物、全局特效、难度系统

       collectibles: 当前场景中可收集物列表（金币、宝石、道具）。
       activeEffects: 全局特效列表（如全屏震动、慢动作）。
       difficultyLevel: 当前难度等级（1-10），随高度递增。
       difficultyAccumulator: 难度递增累加器。
       ============================================================ */
    private List<Collectible> collectibles = new CopyOnWriteArrayList<>();
    private List<String> activeEffects = new CopyOnWriteArrayList<>();
    private int difficultyLevel = 1;
    private double difficultyAccumulator = 0.0;
    private double nextCollectibleSpawnY = 0;  // 下一个收集物生成的摄像机Y坐标

    public GameState() {}

    /* ============================================================
       Getters & Setters
       ============================================================ */

    public Map<String, Player> getPlayers() { return players; }
    public void setPlayers(Map<String, Player> players) { this.players = players != null ? players : new ConcurrentHashMap<>(); }

    public List<Obstacle> getObstacles() { return obstacles; }
    public void setObstacles(List<Obstacle> obstacles) { this.obstacles = obstacles != null ? obstacles : new CopyOnWriteArrayList<>(); }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public int getFrames() { return frames; }
    public void setFrames(int frames) { this.frames = frames; }

    public double getCameraY() { return cameraY; }
    public void setCameraY(double cameraY) { this.cameraY = cameraY; }

    public double getCameraTargetY() { return cameraTargetY; }
    public void setCameraTargetY(double cameraTargetY) { this.cameraTargetY = cameraTargetY; }

    public double getNextSpawnCameraY() { return nextSpawnCameraY; }
    public void setNextSpawnCameraY(double nextSpawnCameraY) { this.nextSpawnCameraY = nextSpawnCameraY; }

    public double getTimeBonusInterval() { return timeBonusInterval; }
    public void setTimeBonusInterval(double timeBonusInterval) { this.timeBonusInterval = timeBonusInterval; }

    public int getTimeBonusPoints() { return timeBonusPoints; }
    public void setTimeBonusPoints(int timeBonusPoints) { this.timeBonusPoints = timeBonusPoints; }

    public double getTimeBonusAccumulator() { return timeBonusAccumulator; }
    public void setTimeBonusAccumulator(double timeBonusAccumulator) { this.timeBonusAccumulator = timeBonusAccumulator; }

    /* ============================================================
       扩展字段 Getters & Setters
       ============================================================ */

    public List<Collectible> getCollectibles() { return collectibles; }
    public void setCollectibles(List<Collectible> collectibles) { this.collectibles = collectibles != null ? collectibles : new CopyOnWriteArrayList<>(); }

    public List<String> getActiveEffects() { return activeEffects; }
    public void setActiveEffects(List<String> activeEffects) { this.activeEffects = activeEffects != null ? activeEffects : new CopyOnWriteArrayList<>(); }

    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public double getDifficultyAccumulator() { return difficultyAccumulator; }
    public void setDifficultyAccumulator(double difficultyAccumulator) { this.difficultyAccumulator = difficultyAccumulator; }

    public double getNextCollectibleSpawnY() { return nextCollectibleSpawnY; }
    public void setNextCollectibleSpawnY(double nextCollectibleSpawnY) { this.nextCollectibleSpawnY = nextCollectibleSpawnY; }
}
