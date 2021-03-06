package com.github.unchama.player;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.github.unchama.donate.DonateDataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.unchama.gigantic.Gigantic;
import com.github.unchama.gigantic.PlayerManager;
import com.github.unchama.player.build.BuildLevelManager;
import com.github.unchama.player.build.BuildManager;
import com.github.unchama.player.buildskill.BuildSkillManager;
import com.github.unchama.player.dimensionalinventory.DimensionalInventoryManager;
import com.github.unchama.player.fly.FlyManager;
import com.github.unchama.player.gacha.PlayerGachaManager;
import com.github.unchama.player.gachastack.GachaStackManager;
import com.github.unchama.player.gigantic.GiganticManager;
import com.github.unchama.player.gravity.GravityManager;
import com.github.unchama.player.gui.GuiStatusManager;
import com.github.unchama.player.huntingpoint.HuntingPointManager;
import com.github.unchama.player.mana.ManaManager;
import com.github.unchama.player.menu.PlayerMenuManager;
import com.github.unchama.player.mineblock.MineBlockManager;
import com.github.unchama.player.minestack.MineStackManager;
import com.github.unchama.player.moduler.DataManager;
import com.github.unchama.player.moduler.Finalizable;
import com.github.unchama.player.moduler.Initializable;
import com.github.unchama.player.moduler.UsingSql;
import com.github.unchama.player.presentbox.PresentBoxManager;
import com.github.unchama.player.region.RegionManager;
import com.github.unchama.player.seichilevel.SeichiLevelManager;
import com.github.unchama.player.seichiskill.active.CondensationManager;
import com.github.unchama.player.seichiskill.active.ExplosionManager;
import com.github.unchama.player.seichiskill.active.FairyAegisManager;
import com.github.unchama.player.seichiskill.active.MagicDriveManager;
import com.github.unchama.player.seichiskill.active.RuinFieldManager;
import com.github.unchama.player.seichiskill.passive.manarecovery.ManaRecoveryManager;
import com.github.unchama.player.seichiskill.passive.mineboost.MineBoostManager;
import com.github.unchama.player.seichiskill.passive.securebreak.SecureBreakManager;
import com.github.unchama.player.settings.PlayerSettingsManager;
import com.github.unchama.player.sidebar.SideBarManager;
import com.github.unchama.player.time.PlayerTimeManager;
import com.github.unchama.player.toolpouch.ToolPouchManager;
import com.github.unchama.sql.Sql;
import com.github.unchama.util.ClassUtil;
import com.github.unchama.util.Converter;
import com.github.unchama.util.ExperienceManager;

/**各プレイヤーにデータを保存したい時はここにマネージャーを追加します．
 *
 * @author tar0ss
 *
 */
public class GiganticPlayer {

	public static enum ManagerType {
		/**
		 * Managerを追加するときはここに書く．
		 */
		GIGANTIC(GiganticManager.class),
		SETTINGS(PlayerSettingsManager.class),
		GUISTATUS(GuiStatusManager.class),
		MINEBLOCK(MineBlockManager.class),
		SEICHILEVLE(SeichiLevelManager.class),
		MANA(ManaManager.class),
		MANARECOVERY(ManaRecoveryManager.class),
		MENU(PlayerMenuManager.class),
		BUILD(BuildManager.class),
		PLAYERGACHA(PlayerGachaManager.class),
		MINEBOOST(MineBoostManager.class),
		MINESTACK(MineStackManager.class),
		TOOLPOUCH(ToolPouchManager.class),
		EXPLOSION(ExplosionManager.class),
		MAGICDRIVE(MagicDriveManager.class),
		CONDENSATION(CondensationManager.class),
		RUINFIELD(RuinFieldManager.class),
		FAIRYAEGIS(FairyAegisManager.class),
		GRAVITY(GravityManager.class),
		SECUREBREAK(SecureBreakManager.class),
		FLY(FlyManager.class),
		REGION(RegionManager.class),
		PLAYERTIME(PlayerTimeManager.class),
		HUNTINGPOINT(HuntingPointManager.class),
		BUILDLEVEL(BuildLevelManager.class),
		BUILDSKILL(BuildSkillManager.class),
		DIMENSIONALINVENTORY(DimensionalInventoryManager.class),
		PRESENTBOX(PresentBoxManager.class),
		DONATEDATA(DonateDataManager.class),
		GACHASTACK(GachaStackManager.class),
		//EFFECT(SkillEffectManager.class),
		SIDEBAR(SideBarManager.class),//サイドバー表示は必ず最後に，
		;

		private Class<? extends DataManager> managerClass;

		ManagerType(Class<? extends DataManager> managerClass) {
			this.managerClass = managerClass;
		}

		public Class<? extends DataManager> getManagerClass() {
			return managerClass;
		}

	}

	Gigantic plugin = Gigantic.plugin;
	Sql sql = Gigantic.sql;

	public final String name;
	public final UUID uuid;
	private GiganticStatus gs;
	private ExperienceManager expManager;
	// Player型は突然消えることがあるため保持しない

	private LinkedHashMap<Class<? extends DataManager>, DataManager> managermap = new LinkedHashMap<Class<? extends DataManager>, DataManager>();

	public GiganticPlayer(Player player) {
		this.name = Converter.getName(player);
		this.uuid = player.getUniqueId();
		this.expManager = new ExperienceManager(player);
		this.setStatus(GiganticStatus.LODING);
		for (ManagerType mt : ManagerType.values()) {
			try {
				this.managermap.put(mt.getManagerClass(), mt.getManagerClass().getConstructor(GiganticPlayer.class)
						.newInstance(this));
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				plugin.getLogger().warning("Failed to create new Instance of player:" + this.name);
				plugin.getLogger().warning("managertype:" + mt.name());
				e.printStackTrace();
			}
		}
		//Sqlを使用しないクラスに関してloadedFlagをtrueに変更
		for (Class<? extends DataManager> mc : this.managermap.keySet()) {
			if (!ClassUtil.isImplemented(mc, UsingSql.class)) {
				try {
					mc.getMethod("setLoaded", Boolean.class).invoke(this.managermap.get(mc), true);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					plugin.getLogger().warning("Failed to setloaded of player:" + this.name);
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends DataManager> T getManager(Class<T> type) {
		return (T) managermap.get(type);
	}

	public boolean isloaded() {
		for (Class<? extends DataManager> mc : this.managermap.keySet()) {
			if (ClassUtil.isImplemented(mc, UsingSql.class)) {
				try {
					boolean loaded = (Boolean) mc.getMethod("isLoaded").invoke(this.managermap.get(mc));
					if (loaded == false) {
						return false;
					}
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					plugin.getLogger().warning("Failed to save data of player:" + this.name);
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	public boolean isOffline() {
		return Gigantic.plugin.getServer().getPlayer(uuid) == null;
	}

	public void init() {
		this.setStatus(GiganticStatus.INITIALIZE);
		for (Class<? extends DataManager> mc : this.managermap.keySet()) {
			if (ClassUtil.isImplemented(mc, Initializable.class)) {
				try {
					mc.getMethod("init").invoke(this.managermap.get(mc));
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					plugin.getLogger().warning("Failed to run init() of player:" + this.name);
					e.printStackTrace();
				}
			}
		}
		this.setStatus(GiganticStatus.AVAILABLE);
		Player player = PlayerManager.getPlayer(this);
		player.sendMessage(ChatColor.GREEN
				+ "ロードが完了しました．");
		sql.onAvailavle(this);
	}

	public void fin() {
		this.setStatus(GiganticStatus.FINALIZE);
		for (Class<? extends DataManager> mc : this.managermap.keySet()) {
			if (ClassUtil.isImplemented(mc, Finalizable.class)) {
				try {
					mc.getMethod("fin").invoke(this.managermap.get(mc));
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					plugin.getLogger().warning("Failed to run fin() of player:" + this.name);
					e.printStackTrace();
				}
			}
		}
	}

	/**プレイヤーデータを保存します．
	 * このメソッドをプレイヤーのログアウト時に呼び出す場合は，loginflagをfalseにしてください．
	 * 定期セーブ時に呼び出す場合はloginflagをtrueにしてください．
	 *
	 * @param loginflag:
	 */
	public void save(boolean loginflag) {
		if (!loginflag) {
			this.setStatus(GiganticStatus.SAVING);
		}
		for (Class<? extends DataManager> mc : this.managermap.keySet()) {
			if (ClassUtil.isImplemented(mc, UsingSql.class)) {
				try {
					mc.getMethod("save", Boolean.class).invoke(this.managermap.get(mc), loginflag);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					plugin.getLogger().warning("Failed to save data of player:" + this.name);
					e.printStackTrace();
				}
			}
		}
	}

	/**プレイヤーデータのステータスをセットします．
	 *
	 * @param gs
	 */
	private void setStatus(GiganticStatus gs) {
		this.gs = gs;
	}

	/**プレイヤーデータのステータスを取得します．
	 *
	 * @return ステータス
	 */
	public GiganticStatus getStatus() {
		return this.gs;
	}

	/**経験値マネージャーを取得します．
	 *
	 * @return ステータス
	 */
	public ExperienceManager getExpManager(){
		return this.expManager;
	}



	/*
	//３０分間のデータを保存する．
	public MineBlock halfhourblock;




	//MineStack
	//public MineStack minestack;

	//public MineStack minestack = new MineStack();
	//MineStackFlag

	public boolean minestackflag;
	//プレイ時間差分計算用int
	public int servertick;
	//プレイ時間
	public int playtick;
	//キルログ表示トグル
	public boolean dispkilllogflag;
	//全体通知音消音トグル
	public boolean everysoundflag;
	//ワールドガード保護ログ表示トグル
	public boolean dispworldguardlogflag;
	//複数種類破壊トグル
	public boolean multipleidbreakflag;

	//PvPトグル
	public boolean pvpflag;

	//放置時間
	public int idletime;
	//トータル破壊ブロック
	public int totalbreaknum;
	//整地量バー
	public ExpBar expbar;
	//合計経験値
	public int totalexp;
	//経験値マネージャ
	public ExperienceManager expmanager;
	//合計経験値統合済みフラグ
	public byte expmarge;
	//各統計値差分計算用配列
	private List<Integer> staticdata;
	//特典受け取り済み投票数
	public int p_givenvote;
	//投票受け取りボタン連打防止用
	public boolean votecooldownflag;

	//アクティブスキル関連データ
	public ActiveSkillData activeskilldata;

	//MebiusTask
	public MebiusTaskRunnable mebius;

	//ガチャボタン連打防止用
	public boolean gachacooldownflag;

	//インベントリ共有トグル
	public boolean shareinv;
	//インベントリ共有ボタン連打防止用
	public boolean shareinvcooldownflag;

	//サブのホームポイント
	private Location[] sub_home = new Location[SeichiAssist.config.getSubHomeMax()];

	//LV・二つ名表示切替用
	public boolean displayTypeLv;
	//表示二つ名の指定用
	public int displayTitleNo;
	//二つ名解禁フラグ保存用
	public BitSet TitleFlags;
	//二つ名関連用にp_vote(投票数)を引っ張る。(予期せぬエラー回避のため名前を複雑化)
	public int p_vote_forT ;


	//建築LV
	private int build_lv;
	//設置ブロック数
	private int build_count;
	//設置ブロックサーバー統合フラグ
	private byte build_count_flg;
	*/
}
