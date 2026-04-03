# Kaguya Client

Minecraft 1.8.9 向けの Forge クライアント Mod です。  
PvP 補助・ビジュアル・ユーティリティなど **70 以上のモジュール** と **16 のチャットコマンド** を搭載しています。

---

## 動作環境

| 項目 | バージョン |
|---|---|
| Minecraft | 1.8.9 |
| Minecraft Forge | 1.8.9-11.15.1.2318 |
| Java | 8 (JDK 1.8) |
| Mixin | SpongePowered Mixin 0.7.10 |

---

## ビルド方法

```bash
# リポジトリをクローン
git clone https://github.com/xKobayashiChan/Kaguya.git
cd Kaguya

# ビルド (JDK 8 が必要)
./gradlew build
```

ビルド成果物は `build/libs/KaguyaClient-v1.1.0.jar` に出力されます。

### 開発環境での起動

```bash
# ビルド → run/mods にコピー → クライアント起動
./gradlew buildCopyToRunModsAndRun
```

---

## モジュール一覧

### Combat（戦闘）
| モジュール | 説明 |
|---|---|
| AimAssist | エイム補助 |
| AutoClicker | 自動クリック |
| HitBox | ヒットボックス拡大 |
| HitSelect | ヒットセレクト |
| KillAura | 自動攻撃 |
| MoreKB | ノックバック増加 |
| Reach | リーチ拡張 |
| TargetStrafe | ターゲット周囲を旋回 |
| Velocity | ノックバック軽減 |
| Wtap | W タップ |

### Movement（移動）
| モジュール | 説明 |
|---|---|
| Eagle | スニークブリッジ |
| Fly | 飛行 |
| Jesus | 水上歩行 |
| KeepSprint | スプリント維持 |
| LongJump | ロングジャンプ |
| NoSlow | 減速無効 |
| SafeWalk | 端からの落下防止 |
| Scaffold | 自動ブリッジ |
| Speed | 移動速度上昇 |
| Sprint | 自動スプリント |
| NoJumpDelay | ジャンプ遅延無効 |

### Visual（表示）
| モジュール | 説明 |
|---|---|
| BedESP | ベッドのハイライト表示 |
| Chams | エンティティの透過表示 |
| ChestESP | チェストのハイライト表示 |
| ESP | エンティティのアウトライン表示 |
| FullBright | フルブライト |
| ItemESP | ドロップアイテムのハイライト表示 |
| NameTags | 名前タグの拡大表示 |
| Tracers | エンティティへの線描画 |
| Trajectories | 投擲物の軌道表示 |
| Xray | ブロック透視 |
| NoHurtCam | ダメージ時の画面揺れ無効 |
| ViewClip | カメラクリップ |

### HUD（画面表示）
| モジュール | 説明 |
|---|---|
| ClientHUD | クライアント HUD |
| HUD | モジュールリスト表示 |
| Indicators | インジケーター |
| Radar | レーダー |
| TargetHUD | ターゲット情報表示 |

### Player（プレイヤー）
| モジュール | 説明 |
|---|---|
| AntiAFK | AFK 検知防止 |
| AntiDebuff | デバフ除去 |
| AntiFireball | ファイアボール自動打ち返し |
| AntiObbyTrap | 黒曜石トラップ対策 |
| AntiVoid | 落下死防止 |
| AutoAnduril | アンドゥリル自動使用 |
| AutoBlockIn | 自動ブロックイン |
| AutoHeal | 自動回復 |
| AutoTool | 最適ツール自動選択 |
| Blink | パケット遅延 |
| FastPlace | 高速ブロック設置 |
| Freeze | 位置固定 |
| GhostHand | ゴーストハンド |
| InvManager | インベントリ管理 |
| InvWalk | インベントリ中の移動 |
| LagRange | ラグレンジ |
| MCF | ミドルクリックフレンド |
| NoFall | 落下ダメージ無効 |
| NoHitDelay | 攻撃遅延無効 |
| NoRotate | サーバー側の回転無効 |
| Refill | ホットバー自動補充 |
| SpeedMine | 採掘速度上昇 |
| VclipCommand | 垂直テレポート |

### Utility（その他）
| モジュール | 説明 |
|---|---|
| AntiObfuscate | 難読化テキスト無効化 |
| BedNuker | ベッド自動破壊 |
| BedTracker | ベッド位置追跡 |
| ChatCopy | チャットコピー |
| ChestStealer | チェスト自動取得 |
| GuiModule | クリック GUI |
| InventoryClicker | インベントリ自動クリック |
| LightningTracker | 雷追跡 |
| NickHider | ニックネーム隠蔽 |
| Spammer | チャットスパマー |

---

## コマンド一覧

チャット内でプレフィックス付きコマンドを入力して使用します。

| コマンド | 説明 |
|---|---|
| `.bind` | モジュールにキーバインドを設定 |
| `.toggle` | モジュールの有効/無効を切り替え |
| `.config` | 設定の保存/読み込み |
| `.friend` | フレンドリストの管理 |
| `.target` | ターゲットリストの管理 |
| `.help` | ヘルプを表示 |
| `.list` | モジュール一覧を表示 |
| `.module` | モジュール設定の変更 |
| `.hide` / `.show` | HUD からモジュールを非表示/表示 |
| `.chatcopy` | チャットコピー設定 |
| `.denick` | ニックネーム解除 |
| `.ign` | IGN コマンド |
| `.item` | アイテムコマンド |
| `.player` | プレイヤーコマンド |
| `.vclip` | 垂直テレポート |

---

## プロジェクト構成

```
src/main/java/
├── com/example/lexiyaddons/     # メインパッケージ
│   ├── LexiyAddons.java         # Forge @Mod エントリーポイント
│   ├── Myau.java                # 初期化ハブ (モジュール・コマンド・マネージャーの登録)
│   ├── init/                    # Mixin ローダー・起動処理
│   ├── module/                  # モジュールシステム (Module 基底クラス + 70 以上の実装)
│   ├── command/                 # チャットコマンドシステム
│   ├── event/                   # カスタムイベントバス (リフレクションベース)
│   ├── events/                  # 具体的なイベントクラス (24 種類)
│   ├── mixins/                  # Mixin クラス (~43 個、Minecraft のバイトコードに注入)
│   ├── management/              # 状態マネージャー (Rotation, Blink, Friend, Target 等)
│   ├── config/                  # JSON 設定の保存/読み込み
│   ├── property/                # モジュール設定のプロパティシステム
│   ├── ui/                      # クリック GUI
│   ├── util/                    # ユーティリティクラス
│   ├── enums/                   # 列挙型
│   └── data/                    # データ構造
│
└── me/ksyz/accountmanager/      # アカウントマネージャー (Microsoft/Mojang 認証)
```

---

## ライセンス

[Minecraft Forge Public Licence](LICENSE.md)
