import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Simulation extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int FRAMEX = 1000, FRAMEY = 1000, SIZE = 100;
	private static final String SIMNAME = "Simulation v1.0", CSV1NAME = "G13_1.csv", CSV2NAME = "G13_2.csv";
	private static final DecimalFormat TIMEFORMAT = new DecimalFormat("#.00");
	private static int wins, mostFound;
	public static int mode;
	private static double totHap = 0, totComp = 0;
	public boolean isSimulating;

	private Node[][] nodes = new Node[SIZE][SIZE];
	private Agent[] agents = new Agent[1];

	public Simulation(int iteration) {
		setup();
		setFocusable(true);
		isSimulating = true;
		System.out.println("Simulation #" + iteration + " started in scenario " + (mode + 1));
	}

	public void setup() {

		// initialize counters
		wins = mostFound = 0;

		// initialize agents
		for (int i = 0; i < agents.length; i++)
			agents[i] = new Agent(i, FRAMEX, FRAMEY);

		// initialize nodes
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++)
				nodes[i][j] = new Node(i, j, FRAMEX, FRAMEY);
		}

		// pick locations for target nodes
		Integer[] xls = new Integer[100];
		Integer[] yls = new Integer[100];
		for (int i = 0; i < SIZE; i++) {
			xls[i] = i;
			yls[i] = i;
		}
		Collections.shuffle(Arrays.asList(xls)); // x coordinates for targets
		Collections.shuffle(Arrays.asList(yls)); // y coordinates for targets

		// setup targets for agents 0 - 4
		for (int i = 0; i < agents.length * 5; i++) {
			nodes[xls[i]][yls[i]].setTargetID(i);
			nodes[xls[i]][yls[i]].setAgentID(i / 5);
		}
	}

	public void detectAgentCollision() {
		for (int i = 0; i < agents.length; i++) {
			for (int j = 0; j < agents.length; j++) {

				// don't check collision with self
				if (agents[i].getAgentID() == agents[j].getAgentID())
					continue;

				// if its already diverting, leave alone
				if (agents[i].isDiverting || agents[j].isDiverting)
					continue;

				// get center radius, coordinates, velocities
				int x1 = agents[i].getX();
				int y1 = agents[i].getY();
				int x2 = agents[j].getX();
				int y2 = agents[j].getY();
				int velX1 = agents[i].getVelX();
				int velY1 = agents[i].getVelY();

				// check if agents are within 1 unit of each other
				if (Math.hypot(x1 - x2, y1 - y2) <= 10) {

					// check if agent 1 is moving horizontally
					if (velX1 != 0) {

						// check if also moving vertically (diagonal case)
						if (velY1 != 0) {

							// check which side of window is farther
							if (FRAMEX - x1 > x1)
								agents[i].divertPath(new Coordinate(x1 + 5, y1)); // divert right
							else
								agents[i].divertPath(new Coordinate(x1 - 5, y1)); // divert left
						}

						// (horizontal only case)
						else {

							// check which side of window is farther
							if (FRAMEY - y1 > y1)
								agents[i].divertPath(new Coordinate(x1, y1 - 5)); // divert up
							else
								agents[i].divertPath(new Coordinate(x1, y1 + 5)); // divert down
						}
					}

					// check if agent 1 is moving vertically (vertical case)
					else if (velY1 != 0) {

						// check which side of window is farther
						if (FRAMEX - x1 > x1)
							agents[i].divertPath(new Coordinate(x1 + 5, y1)); // divert right
						else
							agents[i].divertPath(new Coordinate(x1 - 5, y1)); // divert left
					}

					else {
						/* non moving collision, should never happen */}
				}
			}
		}
	}

	public void detectTargetCollision() {
		for (int k = 0; k < agents.length; k++) {
			for (int i = 0; i < SIZE; i++) {
				for (int j = 0; j < SIZE; j++) {

					// get values for calculation
					int aX = agents[k].getX();
					int aY = agents[k].getY();
					int tX = nodes[i][j].getX();
					int tY = nodes[i][j].getY();
					boolean isWithinRadius = false;

					// calculate if target is within agent's search radius
					if (Math.pow(tX - aX, 2) + Math.pow(tY - aY, 2) <= Math.pow(agents[k].getRadius(), 2))
						isWithinRadius = true;
					else
						continue;

					// if target belongs to this agent
					if (nodes[i][j].getAgentID() == agents[k].getAgentID()) {

						// check if target is within search radius
						if (isWithinRadius) {
							agents[k].addTarget(nodes[i][j]); // add to list of targets
							nodes[i][j].clearTarget(); // remove targetID and agentID
							nodes[i][j].setIsFound(true);

							// check if new highest amount of targets found
							if (agents[k].getNumFound() > mostFound)
								mostFound = agents[k].getNumFound();
						}
					}
				}
			}
		}
	}

	public void update() {
		detectAgentCollision();
		detectTargetCollision();

		// loop through all agents
		for (int i = 0; i < agents.length; i++) {
			if (agents[i].isActive) {
				checkBroadcast(agents[i]);
				agents[i].update();
			}
		}
	}

	public void checkBroadcast(Agent a) {
		if (a.getBroadcast() != null) {

			// check for win condition, on win stop simulation
			if (a.getBroadcast().body.equals("won")) {
				a.clearBroadcast();
				wins++;
				System.out.println(String.valueOf("Agent: " + a.getAgentID()) + " (" + a.getColor()
						+ ") found all targets. Step count: " + a.getStepCount());

				// scenario 1 = 5 wins, else 1 win
				if ((mode == 1 && wins == agents.length) || mode != 1)
					isSimulating = false;
			}

			// check for path completion
			else if (a.getBroadcast().body.equals("done path")) {
				System.out.println(String.valueOf("Agent: " + a.getAgentID()) + " (" + a.getColor()
						+ ") has comepleted it's path.");
			}
		}
	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		super.paint(g);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		// paint the targets
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++)
				nodes[i][j].draw(g2d);
		}

		// paint the agents
		for (int i = 0; i < agents.length; i++)
			agents[i].draw(g2d);
	}

	public static void main(String args[]) throws InterruptedException, IOException {

		Scanner in = new Scanner(System.in);
		int iterations, simSpeed;
		mode = 0;
		iterations = 1;
		simSpeed = 50;

		double totalRuntime = System.nanoTime();

		Simulation sim = null;

		for (int i = 0; i < iterations; i++) {
			System.out.println();

			JFrame frame = new JFrame(SIMNAME + " - scenario " + (mode + 1));
			sim = new Simulation(i);


			frame.add(sim);
			frame.setSize(FRAMEX + 15, FRAMEY + 40); // offset so all cells show in window
			frame.setResizable(false);
			frame.setLocation(900, 10); // moved window to right to see command window
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


			while (sim.isSimulating) {
				sim.update();

				// if no window, then don't do these
				if (simSpeed > 0) {
					sim.repaint();
					Thread.sleep(simSpeed);
				}
			}

		}


		// calculate time elapsed for all iterations
		totalRuntime = (System.nanoTime() - totalRuntime) / 1000000000.0;

		System.out.println();
		System.out.println("Simulation complete in scenario " + (mode + 1) + " with " + iterations + " iteration(s) and animation speed " + -(simSpeed / 10 - 5) + ".");
		System.out.println("Total runtime: " + TIMEFORMAT.format(totalRuntime) + "s");

		System.out.print("\nPress any key to exit...");
		try {
			System.in.read(new byte[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}

		in.close();
	}
}
