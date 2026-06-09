# WallRunner v3.0 — UML 建模设计文档（已修正版）

> **版本**: 3.0.0-fix1 | **日期**: 2026-06-09  
> **目标**: 严格遵循软件工程设计理念，UML 建模与代码实现保持一致  
> **修正说明**: 本版本已根据代码实际实现修正了状态图、类图关系、设计模式归类等问题

---

## 一、设计原则与架构升级

### 1.1 SOLID 原则贯彻

| 原则 | 原 v2.0 问题 | v3.0 解决方案 | UML 建模意义 |
|------|-------------|--------------|-------------|
| **S**ingle Responsibility | `GamePhysics` 承担过多职责（物理计算+渲染+输入） | 拆分为 12 个独立子系统接口 + `PhysicsEngine` 调度器 | 每个子系统一个接口，类图清晰展示职责边界 |
| **O**pen/Closed | 新增物理功能需修改 `GamePhysics` | 新增子系统实现接口即可，调度器通过构造器注入 | 类图中展示实现关系，不影响已有结构 |
| **L**iskov Substitution | 无抽象层，无法替换实现 | 所有服务/引擎/子系统均定义接口 | 类图中展示泛化关系，便于替换测试 |
| **I**nterface Segregation | 大块类直接依赖具体实现 | 客户端/服务端/物理层均按职责拆分接口 | 接口粒度适中，UML 类图不臃肿 |
| **D**ependency Inversion | `GameController` 直接 `new` 具体服务 | 构造器依赖注入接口 | 类图中展示依赖关系（虚线箭头） |

### 1.2 设计模式应用

| 模式 | 应用位置 | UML 建模意义 | 代码验证 |
|------|---------|-------------|---------|
| **Strategy（策略）** | `IPhysicsEngine` + 12 个子系统接口 | 类图中展示接口与多实现的泛化关系 | ✅ 真实落地：PhysicsEngine 组合各子系统接口 |
| **Observer（观察者）** | `GameEventBus` + `GameEvent` 继承体系 | 类图中展示事件继承树，交互图展示发布-订阅时序 | ⚠️ 框架已搭建，实际使用场景有限（目前仅 PhaseChangeEvent 一处） |
| **Singleton（单例）** | `GameEventBus` | 类图中标注 `<<singleton>>` 构造型 | ✅ 真实落地：private static final INSTANCE |
| **Factory Method** | `PhysicsEngine.createDefault()` | 类图中展示创建型方法的返回类型为接口 | ⚠️ 真实但硬编码实现类，违反 OCP |
| **Dependency Injection** | `GameController` / `MenuController` / `GameWebSocketHandler` 构造器注入 | 交互图展示对象组装时序 | ✅ 真实落地：Spring 构造器注入 |
| **Static Proxy** | `GamePhysics` 委托给 `PhysicsEngine` | 类图中展示 `GamePhysics` → `PhysicsEngine` 的委托关系 | ✅ 真实落地：静态方法委托给静态持有的 ENGINE |

> **注意**：`StateManager` 是 Spring 管理的单例 Bean（由容器控制生命周期），**不是**通过 Singleton 设计模式（私有构造器+静态 getInstance）实现的。文档中不再将其归类为 Singleton 模式。

> **注意**：`GamePhysics` 是**静态代理（Static Proxy）/ 兼容门面（Compatibility Facade）**，用于兼容 v2.0 的静态方法调用，而非传统 Facade 模式。传统 Facade 通常以实例形式存在，为复杂子系统提供简化接口。

---

## 二、模块分层（AKF 扩展立方体）

```
┌─────────────────────────────────────────────────────────────┐
│  Z 层：表现与交互 (Presentation) — 客户端独有                    │
│  ├─ IRenderer / Renderer          — Canvas 渲染管线          │
│  ├─ IInputHandler / InputService    — 键盘/鼠标/触摸输入       │
│  ├─ IGameLoop / GameLoopService     — 120 FPS 调度器         │
│  └─ Controller 层                   — FXML 事件绑定            │
├─────────────────────────────────────────────────────────────┤
│  Y 层：共享核心 (Shared-Core) — 客户端与服务端复用               │
│  ├─ entity.*                        — IEntity / Player / Obstacle / Collectible / GameState │
│  ├─ physics.*                       — IPhysicsEngine / PhysicsEngine / GamePhysics(Static Proxy) │
│  ├─ physics.subsystem.*             — 12 个子系统接口与实现     │
│  ├─ event.*                         — GameEventBus / GameEvent 继承体系 │
│  └─ constants.*                     — GameConstants             │
├─────────────────────────────────────────────────────────────┤
│  X 层：基础设施 (Infrastructure) — 服务端独有                  │
│  ├─ IDedicatedService / DedicatedService — 权威服务器模式      │
│  ├─ IRelayService / RelayService         — P2P 转发模式        │
│  ├─ IRoomManager / RoomManager           — 房间生命周期        │
│  ├─ ISessionManager / SessionManager     — WebSocket 会话注册表 │
│  └─ GameWebSocketHandler                 — 消息路由总线        │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、UML 建模核心内容索引

### 3.1 用例图（Use Case Diagram）— 3 种

| 文件名 | 核心内容 | 建模重点 |
|--------|---------|---------|
| `usecase_system.puml` | 系统整体用例 | 玩家/房主/访客三种 Actor，单人/联机/房间三种用例包，include/extend 关系 |
| `usecase_network.puml` | 联机网络用例 | 客户端A/B、公共服务器、房主客户端四种 Actor，心跳/预测/调和/重连用例 |
| `usecase_room.puml` | 房间管理用例 | 创建/加入/离开/销毁房间，颜色分配，掉线检测用例 |

**用例图设计要点**：
- Actor 按角色划分（玩家/房主/访客/服务端）
- 用例按功能域分组（游戏核心/网络通信/房间管理）
- `<<include>>` 表示必然包含关系（如所有模式均包含跳跃）
- `<<extend>>` 表示可选扩展关系（如观战模式扩展自死亡事件）
- **已修正**：`UC8`（玩家碰撞互动）与 `UC5`（跳跃换墙）为独立用例，非 extend 关系

### 3.2 类图（Class Diagram）— 3 种

| 文件名 | 核心内容 | 建模重点 |
|--------|---------|---------|
| `class_entity.puml` | shared-core 实体层 | IEntity 泛化关系，GameState 组合 Player/Obstacle/Collectible（实心菱形） |
| `class_physics.puml` | 物理引擎子系统 | IPhysicsEngine 实现关系，PhysicsEngine 组合 12 个子系统接口 + eventBus，GamePhysics 静态代理委托 |
| `class_client.puml` | 客户端服务层 | 6 个服务接口，GameController 依赖注入 7 个接口（仅带参构造器），展示依赖关系（虚线箭头） |

**类图设计要点**：
- **泛化关系**（Generalization）：`Player --|> IEntity`（空心三角实线）
- **实现关系**（Realization）：`PhysicsEngine ..|> IPhysicsEngine`（空心三角虚线）
- **组合关系**（Composition）：`PhysicsEngine ◆-- CollisionSystem`（实心菱形）— 同生共死，构造器注入
- **依赖关系**（Dependency）：`GameController ..> IRenderer`（虚线箭头）
- **关联关系**（Association）：`GameState --> Player`（实线箭头）
- **已修正**：PhysicsEngine 与子系统为组合关系（`*--`），非依赖关系（`-->`）
- **已修正**：PhysicsEngine 补充 eventBus 字段
- **已修正**：GamePhysics 标注为静态代理（Static Proxy），非 Facade
- **已修正**：GameController 删除默认构造器声明，仅保留 Spring 注入的带参构造器

### 3.3 交互图（Sequence Diagram）— 3 种

| 文件名 | 核心内容 | 建模重点 |
|--------|---------|---------|
| `sequence_single_start.puml` | 单人游戏启动 | MenuController → GameController → 各服务初始化时序，展示依赖注入组装 |
| `sequence_dedicated_sync.puml` | Dedicated 状态同步 | 客户端输入 → GamePhysics 静态代理 → PhysicsEngine → 广播状态 → 客户端调和的完整闭环 |
| `sequence_collision.puml` | 玩家碰撞处理 | PhysicsEngine 调度 MovementSystem → CollisionSystem → PlayerCollisionResolver → KnockbackSystem 的链式调用 |

**交互图设计要点**：
- **生命线**（Lifeline）：每个对象一条垂直虚线
- **激活条**（Activation）：方法执行期间的长条
- **消息类型**：同步消息（实线箭头）、返回消息（虚线箭头）、自调用（回环箭头）
- **alt/opt** 片段：条件分支（如碰撞类型判断）
- **注释框**：说明 reconcile() 算法、tick 频率等关键信息
- **已修正**：sequence_dedicated_sync.puml 增加 `GamePhysics` 生命线，展示静态代理的委托过程

### 3.4 状态图（State Diagram）— 3 种

| 文件名 | 核心内容 | 建模重点 |
|--------|---------|---------|
| `state_player.puml` | 玩家生命周期 | Idle → Climbing → Jumping → KnockedBack → Returning → Dead → Spectating → Eliminated |
| `state_game_phase.puml` | 游戏阶段 | Menu → Playing → Paused → GameOver → Menu |
| `state_room.puml` | 房间生命周期 | Waiting → Playing → GameOver → Dissolved |

**状态图设计要点**：
- **状态**（State）：圆角矩形，标注进入/退出/活动动作
- **转换**（Transition）：箭头标注触发事件 [守卫条件] / 动作
- **复合状态**（Composite State）：Playing 内含 Normal/DifficultyUp/CollectibleSpawn 子状态（概念模型）
- **初始/终止**：黑色实心圆 / 牛眼
- **已修正**：所有状态图底部添加注释，明确说明代码中未使用显式 State 模式，状态通过字段隐式表达
- **已修正**：state_game_phase.puml 中 Paused 标注为 Player.paused 字段，非 phase 子状态
- **已修正**：state_room.puml 删除 "Empty" 状态，房间创建即绑定房主

### 3.5 活动图（Activity Diagram）— 3 种

| 文件名 | 核心内容 | 建模重点 |
|--------|---------|---------|
| `activity_game_loop.puml` | 游戏主循环 | 串行处理流程，14 个活动节点，分区展示逻辑分组 |
| `activity_join_room.puml` | 加入房间流程 | 分区（Partition）展示客户端/服务端处理边界，条件分支判断 |
| `activity_death_respawn.puml` | 死亡重生流程 | 泳道式分区，生命判断分支，状态重置注释 |

**活动图设计要点**：
- **活动节点**（Action）：圆角矩形
- **判断/合并**（Decision/Merge）：菱形
- **分区**（Partition）：虚线框标注处理边界（客户端/服务端/游戏引擎/DeathSystem）
- **对象流**（Object Flow）：展示 GameState 在各活动间的传递
- **已修正**：activity_game_loop.puml 删除 fork/join 符号，改为串行流程分区，避免误导读者认为存在并发
- **已修正**：activity_death_respawn.puml 修正流程与 DeathSystem.java 代码一致：checkDeathLine() → applyDeath() → respawnPlayer() / spectator

---

## 四、接口清单（便于 UML 建模快速索引）

### 4.1 shared-core 接口

| 接口 | 职责 | 实现类 | UML 关系 |
|------|------|--------|---------|
| `IEntity` | 实体标记 | Player, Obstacle, Collectible | 泛化 |
| `IPhysicsEngine` | 物理调度 | PhysicsEngine, GamePhysics | 实现 + 静态代理 |
| `IMovementSystem` | 玩家移动 | MovementSystem | 实现 |
| `ICollisionDetector` | AABB 碰撞检测 | CollisionSystem | 实现 |
| `IObstacleManager` | 障碍物管理 | ObstacleManager | 实现 |
| `ICollectibleManager` | 收集物管理 | CollectibleManager | 实现 |
| `ICameraSystem` | 摄像机跟随 | CameraSystem | 实现 |
| `IKnockbackSystem` | 击退动画 | KnockbackSystem | 实现 |
| `IInvincibilitySystem` | 无敌计时 | InvincibilitySystem | 实现 |
| `IDeathSystem` | 死亡重生 | DeathSystem | 实现 |
| `IScoreCalculator` | 分数计算 | ScoreSystem | 实现 |
| `IDifficultyManager` | 难度递增 | DifficultyManager | 实现 |
| `IPlayerCollisionResolver` | 玩家碰撞解析 | PlayerCollisionResolver | 实现 |
| `IInputHandler` | 输入处理 | InputHandler | 实现 |

### 4.2 客户端接口

| 接口 | 职责 | 实现类 | UML 关系 |
|------|------|--------|---------|
| `IRenderer` | Canvas 渲染 | Renderer | 实现 |
| `IStateManager` | 本地状态管理 | StateManager | 实现（Spring Bean，非 Singleton 模式） |
| `IWebSocketClient` | WebSocket 通信 | WebSocketClientService | 实现 |
| `IInputHandler` | 输入抽象 | InputService | 实现 |
| `IGameLoop` | 游戏循环 | GameLoopService | 实现 |
| `ILocalPhysicsEngine` | 本地物理 | LocalPhysicsEngine | 实现 |
| `IPredictor` | 状态预测 | Predictor | 实现 |

### 4.3 服务端接口

| 接口 | 职责 | 实现类 | UML 关系 |
|------|------|--------|---------|
| `IDedicatedService` | 权威服务器 | DedicatedService | 实现 |
| `IRelayService` | P2P 转发 | RelayService | 实现 |
| `IRoomManager` | 房间管理 | RoomManager | 实现 |
| `ISessionManager` | 会话管理 | SessionManager | 实现 |

---

## 五、事件系统（Observer 模式）

```
GameEvent (abstract)
├── PlayerJumpEvent
├── PlayerDeathEvent
├── CollisionEvent
├── PhaseChangeEvent
├── ScoreChangeEvent
└── [可扩展更多事件]

GameEventBus (Singleton)
  ├── subscribe(EventType, Consumer)
  ├── unsubscribe(EventType, Consumer)
  └── publish(GameEvent)
```

**UML 建模意义**：
- 类图中展示 `GameEvent` 抽象类与具体事件的泛化树
- 交互图中展示 `publish()` → `listener.accept()` 的观察者调用链
- 状态图中事件作为状态转换的触发条件

**当前实现状态**：
- ✅ 框架完整：GameEventBus 实现了单例、订阅、发布、取消订阅
- ⚠️ 使用有限：目前仅 `PhysicsEngine.startGame()` 中发布 `PhaseChangeEvent` 一处实际使用
- 📋 待扩展：建议在各子系统中增加事件发布（如 CollisionSystem 发布 CollisionEvent，DeathSystem 发布 PlayerDeathEvent）

---

## 六、向后兼容性说明

v3.0 所有重构均通过 **接口 + 静态代理** 模式保证零功能损失：

1. `GamePhysics` 保留为静态代理，所有静态方法委托给 `PhysicsEngine`
2. `GameController` 由 Spring 容器自动装配具体实现（构造器注入）
3. `WebSocketClientService` / `StateManager` 由 Spring 容器单例化管理
4. 所有原有 FXML/CSS/资源文件未做修改
5. Maven 模块结构不变（shared-core / server / client）

---

## 七、UML 文件清单

```
docs/uml/
├── usecase_system.puml          # 用例图1: 系统整体（已修正UC8关系）
├── usecase_network.puml         # 用例图2: 联机网络
├── usecase_room.puml            # 用例图3: 房间管理
├── class_entity.puml            # 类图1: 实体层
├── class_physics.puml           # 类图2: 物理子系统（已修正组合关系+eventBus+静态代理）
├── class_client.puml            # 类图3: 客户端服务层（已修正构造器）
├── sequence_single_start.puml   # 交互图1: 单人启动
├── sequence_dedicated_sync.puml # 交互图2: 状态同步（已修正GamePhysics生命线）
├── sequence_collision.puml      # 交互图3: 碰撞处理
├── state_player.puml            # 状态图1: 玩家生命周期（已修正：字段组合实现）
├── state_game_phase.puml        # 状态图2: 游戏阶段（已修正：Paused为字段非子状态）
├── state_room.puml              # 状态图3: 房间生命周期（已修正：删除Empty状态）
├── activity_game_loop.puml      # 活动图1: 主循环（已修正：串行流程）
├── activity_join_room.puml      # 活动图2: 加入房间
└── activity_death_respawn.puml  # 活动图3: 死亡重生（已修正：与DeathSystem代码一致）
```

---

> **文档生成日期**: 2026-06-09  
> **修正说明**: 本版本已根据代码实际实现修正了状态图、类图关系、设计模式归类等问题  
> **适用工具**: PlantUML / StarUML / Visual Paradigm / Enterprise Architect  
> **渲染方式**: 将 `.puml` 文件导入 PlantUML 在线编辑器或 IDE 插件即可生成图形
