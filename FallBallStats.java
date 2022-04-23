import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Collections;

class Player {
	String name;
	int id;
	int match;
	int win;
	Boolean tempWin;
	boolean exist;
	int adjustMatch;	// 審判回数補正
	int adjustWin;		// 審判回数補正

	Player(String name) {
		this.name = name;
	}
	public int getMatch() {
		return match - adjustMatch;
	}
	public int getWin() {
		return win - adjustWin;
	}
	public double getRate() {
		return Double.parseDouble(FallBallStats.frame.calRate(getWin(), getMatch()));
	}
}

class PlayerList {
	ArrayList<Player> list = new ArrayList<Player>();

	public void clear() {
		for (Player player : list) {
			player.exist = false;
		}
	}
	public void add(Player player) {
		list.add(player);
	}

	public Player getByName(String name) {
		for (Player player : list) {
			if (name.equals(player.name)) return player;
		}
		return null;
	}
	public int getCurrentPlayerCount() {
		int c = 0;
		for (Player player : list) {
			if (player.exist) c += 1;
		}
		return c;
	}
	/*
	public Player getByCurrentId(int id) {
		for (Player player : list) {
			if (id == player.id) return player;
		}
		return null;
	}
	*/
	public String getRanking(int sort) {
		if (sort == 0) {
			Collections.sort(list, new PlayerComparatorWin());
		} else {
			Collections.sort(list, new PlayerComparatorRate());
		}
		String str = "<html>";
		int no = 0;
		for (Player player : list) {
			if (player.getMatch() >= FallBallStats.frame.ranking_filter_flg) {
				no += 1;
				str = str + pad(no)+" ";
				str = str + pad(player.getWin()) + "/";
				str = str + pad(player.getMatch()) + "(";
				String rate_str = String.valueOf(player.getRate());
				String[] rate_sp = rate_str.split("\\.", 2);
				if (rate_sp[0].length() == 1) { rate_str = "0" + rate_str; }
				if (rate_sp[1].length() == 1) { rate_str = rate_str + "0"; }
				str = str + rate_str + "%) ";

				str = str + player.name + "<br>";
			}
		}
		str = str + "</html>";
		return str;
	}

	static String pad(int v) {
		return v < 10 ? "0"+v : ""+v;
	}

	static class PlayerComparatorWin implements Comparator<Player> {
		@Override
		public int compare(Player p1, Player p2) {
			return p1.getWin() > p2.getWin() ? -1 : 1;
		}
	}
	static class PlayerComparatorRate implements Comparator<Player> {
		@Override
		public int compare(Player p1, Player p2) {
			return p1.getRate() > p2.getRate() ? -1 : 1;
		}
	}
}

public class FallBallStats extends JFrame{
  static FallBallStats frame;
  static PlayerlogThread playerlogthread;
  static String path_str;
  static String fontFamily = "Meiryo UI";
  static PlayerList playerList = new PlayerList();

  public static void main(String[] args) throws Exception {
//	UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
//	UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
	UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
  	int pt_x = 10;
  	int pt_y = 10;
  	int size_x = 600;
  	int size_y = 230;
  	try{
  	  File file = new File("window_pt_size.ini");
  	  BufferedReader br = new BufferedReader(new FileReader(file));
  	  String str;
  	  String[] value;
  	  while((str = br.readLine()) != null) {
  	  	value = str.split(" ", 4);
  	  	pt_x = Integer.parseInt(value[0]);
  	  	pt_y = Integer.parseInt(value[1]);
  	  	size_x = Integer.parseInt(value[2]);
  	  	size_y = Integer.parseInt(value[3]);
  	  }
  	  br.close();

  	  file = new File("path.ini");
  	  br = new BufferedReader(new FileReader(file));
  	  while((str = br.readLine()) != null) {
  	  	path_str = str;
  	  }
  	  br.close();
	} catch(FileNotFoundException e) { System.exit(1);
	} catch(IOException e) { System.exit(1); }

	frame = new FallBallStats();
	frame.setResizable(true);
	frame.setBounds(pt_x, pt_y, size_x, size_y);
	frame.setTitle("Custom Fall Ball Stats");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
  }

  private JPanel p;
  static JLabel count_label;
  static String my_name;
  static JLabel read_date;
  static JLabel fliper1;
  static JLabel fliper2;
  private JButton shinpan_win;
  private JButton shinpan_lose;
  private JLabel ranking_label;
  static JLabel ranking;
  private JButton ranking_sort;
  static int ranking_sort_flg;
  private JButton ranking_filter;
  static int ranking_filter_flg;
  static JComboBox<String> shinpan_list;

  FallBallStats() {
	p = new JPanel();
	p.setLayout(null);
	my_name = "";
	ranking_sort_flg = 0;
	ranking_filter_flg = 0;

	count_label = new JLabel("0勝 / 0試合 (0.0%)");
	count_label.setFont(new Font(fontFamily, Font.BOLD, 20));
	count_label.setBounds(15, 20, 300, 20);
	p.add(count_label);

	read_date = new JLabel("起動");
	read_date.setFont(new Font(fontFamily, Font.PLAIN, 14));
	read_date.setBounds(15, 80, 200, 20);
	p.add(read_date);

	fliper1 = new JLabel("青パネル: ?");
  	fliper1.setFont(new Font(fontFamily, Font.BOLD, 16));
	fliper1.setBounds(15, 50, 150, 20);
	p.add(fliper1);
	fliper2 = new JLabel("黄パネル: ?");
  	fliper2.setFont(new Font(fontFamily, Font.BOLD, 16));
	fliper2.setBounds(140, 50, 150, 20);
	p.add(fliper2);

	ranking_label = new JLabel("【ランキング】");
  	ranking_label.setFont(new Font(fontFamily, Font.BOLD, 14));
	ranking_label.setBounds(295, 20, 200, 20);
	p.add(ranking_label);

	ranking = new JLabel("");
  	ranking.setFont(new Font(fontFamily, Font.PLAIN, 14));
	ranking.setBounds(295, 45, 500, 2000);
	ranking.setVerticalAlignment(JLabel.TOP);
	p.add(ranking);

	ranking_sort = new JButton("勝利数順");
	ranking_sort.setFont(new Font(fontFamily, Font.BOLD, 13));
	ranking_sort.setBounds(375, 15, 85, 25);
	ranking_sort.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	  	if (ranking_sort_flg == 0) {
	  		ranking_sort_flg = 1;
	  		ranking_sort.setText("勝率順");
	  	} else {
	  		ranking_sort_flg = 0;
	  		ranking_sort.setText("勝利数順");
	  	}
		playerlogthread.displayRanking();
	  }
	});
	p.add(ranking_sort);

	ranking_filter = new JButton("全表示");
	ranking_filter.setFont(new Font(fontFamily, Font.BOLD, 13));
	ranking_filter.setBounds(465, 15, 105, 25);
	ranking_filter.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	  	switch(ranking_filter_flg) {
	  		case 0:
	  			ranking_filter_flg = 10;
	  			ranking_filter.setText("10試合以上");
	  			break;
	  		case 10:
	  			ranking_filter_flg = 20;
	  			ranking_filter.setText("20試合以上");
	  			break;
	  		case 20:
	  			ranking_filter_flg = 25;
	  			ranking_filter.setText("25試合以上");
	  			break;
	  		case 25:
	  			ranking_filter_flg = 30;
	  			ranking_filter.setText("30試合以上");
	  			break;
	  		case 30:
	  			ranking_filter_flg = 0;
	  			ranking_filter.setText("全表示");
	  			break;

	  	}
		playerlogthread.displayRanking();
	  }
	});
	p.add(ranking_filter);

	shinpan_list = new JComboBox<String>();
	shinpan_list.setFont(new Font("メイリオ", Font.BOLD, 12));
	shinpan_list.setBounds(120, 150, 150, 25);
	p.add(shinpan_list);

	shinpan_win = new JButton();
	make_button_func(shinpan_win, "審判(勝ち)", 0);
	shinpan_win.setBounds(15, 120, 100, 25);
	shinpan_lose = new JButton();
	make_button_func(shinpan_lose, "審判(負け)", 1);
	shinpan_lose.setBounds(15, 150, 100, 25);

	Container contentPane = getContentPane();
	contentPane.add(p);

	playerlogthread = new PlayerlogThread();
	playerlogthread.start();

	this.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
		try{
		  File file = new File("window_pt_size.ini");
		  PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		  Point pt = frame.getLocationOnScreen();
		  Dimension size = frame.getSize();
		  pw.print(pt.x + " " + pt.y + " " + size.width + " " + size.height);
		  pw.close();
		}catch(IOException e1) {}
		try{
		  File file = new File("result.txt");
		  FileWriter filewriter = new FileWriter(file, true);

		  Date d = new Date();
		  SimpleDateFormat d1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		  filewriter.write("[" + d1.format(d) + "] " + count_label.getText() + "\n");
		  filewriter.close();
		}catch(IOException e1) {}
	  }
	});
  }

  private void updateCount(int flg) {
    String name_selected = (String)shinpan_list.getSelectedItem();
	Player player = playerList.getByName(name_selected);
	if (player == null) return;
	
	int tmpMatch = player.getMatch();
	int tmpWin = player.getWin();
	if (tmpMatch == 0) return;
	if ((flg == 0) && (tmpMatch >= tmpWin)){ // win
		player.adjustMatch += 1;
		player.adjustWin += 1;
	} else if ((flg == 1) && (tmpMatch > tmpWin)){ // lose
		player.adjustMatch += 1;
	}
	playerlogthread.displayRanking();
  }

  static String calRate(int win, int match) {
	if (match == 0) return "0.00";
	BigDecimal win_dec = new BigDecimal(win);
	BigDecimal match_dec = new BigDecimal(match);
	BigDecimal rate = win_dec.divide(match_dec, 4, BigDecimal.ROUND_HALF_UP);
	rate = rate.multiply(new BigDecimal("100"));
	rate = rate.setScale(2, RoundingMode.DOWN);
	return String.valueOf(rate);
  }

  private void make_button_func(JButton button, String name, int flg) {
	button.setText(name);
	button.setFont(new Font(fontFamily, Font.BOLD, 14));
	button.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
			updateCount(flg);
	  }
	});
	p.add(button);
  }
}

class PlayerlogThread extends Thread{
	private Path log_path;
	private int line_cnt;
	private long file_size;
	private int match_status;
	private int match_count;
	private int player_count;
	private boolean first_read;

	public void run() {
		log_path = Paths.get(FallBallStats.frame.path_str);
		line_cnt = 0;
		file_size = 0;
		match_status = 0;
		first_read = false;

		while (true) {
			long cur_file_size = new File(FallBallStats.frame.path_str).length();
			if (file_size > cur_file_size) { line_cnt = 0; match_status = 0; }
			file_size = cur_file_size;

			int tmp_line_cnt = 0;
			try (BufferedReader br = Files.newBufferedReader(log_path, Charset.forName("UTF-8"))) {
				String text;
				while((text = br.readLine()) != null) {
					if (tmp_line_cnt >= line_cnt) {
						getMyName(text);
						getPlayersScore(text);
						getFlipperStatus(text);
					}
					tmp_line_cnt++;
				}
				line_cnt = tmp_line_cnt;
				displayRanking();

				Date d = new Date();
				SimpleDateFormat d1 = new SimpleDateFormat("HH時mm分ss秒");
				FallBallStats.frame.read_date.setText("最終更新: " + d1.format(d));
			} catch (Exception e) {}

			try{
			 	Thread.sleep(10*1000);
			} catch (InterruptedException e) {}
		}
	}

	private void updateShinpanList(){
		FallBallStats.frame.shinpan_list.removeAllItems();
		for (Player player: FallBallStats.playerList.list) {
			FallBallStats.frame.shinpan_list.addItem(player.name);
		}
	}

	static void displayRanking() {
		FallBallStats.frame.ranking.setText(FallBallStats.playerList.getRanking(FallBallStats.frame.ranking_sort_flg));
		Player own = FallBallStats.playerList.getByName(FallBallStats.my_name);
		if (own != null)
			FallBallStats.frame.count_label.setText(own.getWin() + "勝 / " + own.getMatch() + "試合 (" + own.getRate() + "%)");
	}

	private void getMyName(String text) {
		if ((first_read == false) && (text.indexOf("[UserInfo] Player Name:") != -1)) {
			String[] sp = text.split("Player Name: ", 2);
			FallBallStats.my_name = sp[1];
			first_read = true;
		}
	}

	static String dump(byte[] bytes) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < bytes.length; i += 1) {
			b.append(Integer.toString(bytes[i] & 0xff, 16)).append(' ');
		}
		return b.toString();
	}
	private void getPlayersScore(String text) {
		switch(match_status) {
			case 0: // wait for a game
				if (text.indexOf("[HandleSuccessfulLogin] Selected show is event_only_fall_ball_custom_lobby") != -1){
					match_status = 1;
				}
				break;

			case 1: // select custom ball ball cup
				if (text.indexOf("[StateGameLoading] Loading game level scene FallGuy_FallBall_5") != -1){
					// System.out.println("DETECT STARTING FALLBALL");
					FallBallStats.playerList.clear();
					player_count = 0;
					match_status = 2;
				} else if ((text.indexOf("[StateMainMenu] Creating or joining lobby") != -1) ||
							(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)){
					match_status = 0;
				}
				break;

			case 2: // load a fall ball match
				if (text.indexOf("[CameraDirector] Adding Spectator target") != -1) {
					String[] sp1 = text.split("Adding Spectator target ", 2);
					String[] sp2 = sp1[1].split(" with Party ID", 2);
					String[] sp3 = sp2[1].split("playerID: ", 2);

					player_count += 1;
					String player_name = sp2[0].substring(4, sp2[0].length()-6);
					int player_id = Integer.parseInt(sp3[1]);
					Player player = FallBallStats.playerList.getByName(player_name);
					// System.out.println(player_count + " Player " + player_name + " (id=" + player_id + ") spwaned.");

					if (player == null) { // new player
						player = new Player(player_name);
						FallBallStats.playerList.add(player);
					}
					player.id = player_id;
					player.exist = true;
				} else if (text.indexOf("[StateGameLoading] Starting the game.") != -1) {
					// System.out.println("DETECT START FALLBALL");
					match_status = 3;
				} else if ((text.indexOf("[StateMainMenu] Creating or joining lobby") != -1) ||
							(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					// System.out.println("DETECT BACK TO LOBBY");
					match_status = 0;
				}
				break;

			case 3: // start-end a fall ball match
				if (text.indexOf("ClientGameManager::HandleServerPlayerProgress PlayerId") != -1) {
					String[] sp1 = text.split("ClientGameManager::HandleServerPlayerProgress PlayerId=", 2);
					String[] sp2 = sp1[1].split(" is succeeded=", 2);
					int player_id = Integer.parseInt(sp2[0]);

					for (Player player : FallBallStats.playerList.list) {
						if (player_id == player.id && player.exist) {
							if (sp2[1].equals("True")) {
								player.tempWin = Boolean.TRUE;
							} else {
								player.tempWin = Boolean.FALSE;
							}
							break;
						}
					}
				} else if ((text.indexOf("[ClientGameManager] Server notifying that the round is over.") != -1) ||
							(text.indexOf("[StateMainMenu] Creating or joining lobby") != -1) ||
							(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					updateShinpanList();
					match_count += 1;
					// System.out.println("DETECT ROUND OVER " + FallBallStats.playerList.getCurrentPlayerCount() + " players " + match_count + " matches");
					// tempWin 状態を match, win に反映
					for (Player player : FallBallStats.playerList.list) {
						if (player.tempWin != null) {
							player.match += 1;
							player.win += player.tempWin ? 1 : 0;
							player.tempWin = null;
						}
					}
					match_status = 0;
				}
				break;
		}
	}

	private void getFlipperStatus(String text) {
		if ((text.indexOf("SeededRandomisable 12: Flipper has initial flip direction:")) != -1) {
		   if (text.substring(text.length() - 6).equals("North ")) {
			 FallBallStats.frame.fliper1.setText("青パネル: 青側");
		   } else {
			 FallBallStats.frame.fliper1.setText("青パネル: 黄側");
		   }
		} else if ((text.indexOf("SeededRandomisable 13: Flipper has initial flip direction:")) != -1) {
		   if (text.substring(text.length() - 6).equals("North ")) {
			 FallBallStats.frame.fliper2.setText("黄パネル: 青側");
		   } else {
			 FallBallStats.frame.fliper2.setText("黄パネル: 黄側");
		   }
		}
	}
}
