import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import static java.lang.Math.abs;

public class Agent {

	private int agentID;
	private int lazyCoefficient;
	private int x, y, radius, velX, velY, stepCount, numFound;
	private int frameX, frameY;
	private String color;
	private Message broadcast;
	boolean isActive, won, isDiverting, willListen, lies;
	private Random rand = new Random();

	private ArrayList<Node> targets = new ArrayList<Node>(); // found targets go here
	private ArrayList<Coordinate> undiscovered = new ArrayList<Coordinate>();
	private ArrayList<Double> happys = new ArrayList<Double>(); // happiness go here
	private Stack<Coordinate> path = new Stack<Coordinate>(); // path coordinates go here
	private Queue<Message> inbox = new LinkedList<Message>(); // direct messages go here
	private Coordinate currentTarget;

	public Agent(int agentID, int frameX, int frameY) {
		this.agentID = agentID;
		this.frameX = frameX;
		this.frameY = frameY;
		radius = 10; // radar radius
		numFound = stepCount = 0;
		spawn();
		setupPath(Simulation.mode);

		isActive = true;
	}

	public void spawn() {
		// randomly generate location
		x = ThreadLocalRandom.current().nextInt(0, frameX / 10 - 2 * radius);
		y = ThreadLocalRandom.current().nextInt(0, frameY / 10 - 2 * radius);
	}

	public void setupPath(int mode) {
		// set color and create path in reverse
		currentTarget = new Coordinate(0, 0); // set mode 1 start
		path.push(new Coordinate(0, 0)); // end
		switch (agentID) {
			case 0:
				color = "GREEN";
				break;
			case 1:
				color = "BLUE";
				break;
			case 2:
				color = "BLACK";
				break;
			case 3:
				color = "ORANGE";
				break;
			case 4:
				color = "RED";
				break;
		}
		generateUndiscovered();
		generatePath();
	}

	public void generateUndiscovered(){
		for (int i = 0; i <= 100; i+=20) {
			for (int j = 0; j <= 100; j += 20) {
				undiscovered.add(new Coordinate(i, j));
			}
		}
	}

	public void generatePath() {

			if (true) {
				switch (agentID) {
					//Jasindan
					case 0:
						// add path to stack in reverse inorder to unstack it normally
						path.add(new Coordinate(0, 0)); // top left
						path.add(new Coordinate(100, 0));
						path.add(new Coordinate(100, 20));
						path.add(new Coordinate(20, 20));
						path.add(new Coordinate(20, 40));
						path.add(new Coordinate(100, 40));
						path.add(new Coordinate(100, 60));
						path.add(new Coordinate(20, 60));
						path.add(new Coordinate(20, 80));
						path.add(new Coordinate(100, 80));
						path.add(new Coordinate(100, 100));
						path.add(new Coordinate(0, 100)); // bottom left
						break;
					//Amal
					case 1:
						while (undiscovered.size() != 0) {
							int n = rand.nextInt(undiscovered.size());
							path.add(undiscovered.get(n));
							undiscovered.remove(n);
						}
						break;
					//Kalev
					case 2:
						rectangleSweep();
						break;
					//Nam
					case 3:
						circleSweep();
						break;
					//Mingwei
					case 4:
						break;
				}

			}
		}

	private void rectangleSweep() {
		for(int y = 0; y < 10; y++) {
			for(int x = 0; x < 100; x++) {
				path.add(new Coordinate(x,y * 10));
			}
		}
	}

	private void circleSweep() {
		for(int i = 0; i < 2; i++) {
			path.add(new Coordinate(0 + (25 * i),0 + (25 * i)));
			path.add(new Coordinate(100 - (25 * i),0 + (25 * i)));
			path.add(new Coordinate(100 - (25 * i),100 - (25 * i)));
			path.add(new Coordinate(0 + (25 * i),100 - (25 * i)));
		}
	}

	public void setDirection() {
		// decide left, right or none
		if (x > currentTarget.getX())
			velX = -1;
		else if (x < currentTarget.getX())
			velX = 1;
		else
			velX = 0;

		// decide up, down or none
		if (y > currentTarget.getY())
			velY = -1;
		else if (y < currentTarget.getY())
			velY = 1;
		else
			velY = 0;
	}

	public void move() {
		// if we have reached the current target
		if (x == currentTarget.getX() && y == currentTarget.getY()) {
			if (!path.isEmpty()) {
				// get next in line, pop it from stack
				currentTarget = path.pop();
				if (isDiverting)
					isDiverting = false;
			}
		}

		for(int i = 0; i <  path.size(); i++){
			if (x == path.get(i).getX() && y == path.get(i).getY()){
				path.remove(i);
			}
		}

		setDirection(); // set the direction

		// move agent normally
		x += velX;
		y += velY;

		if (velX != 0) {
			stepCount++;
			addHappinessValue();
		}
		if (velY != 0) {
			stepCount++;
			addHappinessValue();
		}
	}

	// if 2 agents collide, 1 diverts
	public void divertPath(Coordinate c) {
		isDiverting = true;
		path.add(currentTarget); // add current target to path again
		currentTarget = c; // change current target to diverting path
		setDirection(); // change direction according to new target
	}

	// if agent learns of a target location, sidetrack
	public void sideTrack(Coordinate c) {
		int coordX = c.getX();
		int coordY = c.getY();
		if(abs(coordX - x) < lazyCoefficient || abs(coordY - y) < lazyCoefficient ) {
			path.add(currentTarget); // add current target to path again
			if (Simulation.mode == 1)
				path.add(new Coordinate(x, y)); // add current location to path, only needed for mode 2
			currentTarget = c; // change current target to target that was given
			setDirection();
		}
	}

	public void checkInbox() {
		// if inbox isn't empty and the agent will listen, side track current path to new target
		if (!inbox.isEmpty() && willListen == true) {
			for (int i = 0; i < inbox.size(); i++) {
				sideTrack(inbox.remove().coordinate);
			}
		}
	}

	public void agentUpdate() {

		checkInbox();
		move();

		// if we have won, no need to broadcast
		if (!won && targets.size() == 5) {
			broadcast = new Message(-1, "won", new Coordinate(x, y));
			won = true;
		}
	}

	public void draw(Graphics2D g2d) {
		// color code agents by rgb, 25% transparent
		switch (agentID) {
			case 0:
				g2d.setColor(new Color(0, 153, 51, 63));
				break;
			case 1:
				g2d.setColor(new Color(0, 102, 255, 63));
				break;
			case 2:
				g2d.setColor(new Color(0, 0, 0, 63));
				break;
			case 3:
				g2d.setColor(new Color(255, 102, 0, 63));
				break;
			case 4:
				g2d.setColor(new Color(255, 0, 0, 63));
				break;
		}
		// draw radar
		g2d.fillOval(x * 10 - radius * 10, y * 10 - radius * 10, 2 * radius * 10, 2 * radius * 10);

		// draw agent
		g2d.fillRect(x * 10 - radius / 2, y * 10 - radius / 2, 10, 10);
	}

	public int getAgentID() {
		return agentID;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getRadius() {
		return radius;
	}

	public int getDiameter() {
		return 2 * radius;
	}

	public int getVelX() {
		return velX;
	}

	public int getVelY() {
		return velY;
	}

	public int getStepCount() {
		return stepCount;
	}

	public int getNumFound() {
		return numFound;
	}

	public void addHappinessValue() {
		happys.add((double) numFound / (stepCount + 1.0));
	}

	public double getHappiness() {
		return happys.get(happys.size() - 1);
	}

	public double getMaxHappiness() {
		return Collections.max(happys);
	}

	public double getMinHappiness() {
		return Collections.min(happys);
	}

	public double getAverageHappiness() {
		double total = 0;
		for (Double h : happys)
			total += (double) h;
		return total;
	}

	public double getVarianceHappiness() {
		double avg = getAverageHappiness(), temp = 0;
		for (Double d : happys)
			temp += (d - avg) * (d - avg);
		return temp / (happys.size() - 1);
	}

	public double getSTDHappiness() {
		return Math.sqrt(getVarianceHappiness());
	}

	public double getCompetitiveness() {
		if (getHappiness() == 0)
			return 0;
		return (getHappiness() - getMinHappiness()) / (getMaxHappiness() - getMinHappiness());
	}

	public boolean getLyingAttribute() {
		return lies;
	}

	public void setLyingAttribute(boolean lie) {
		this.lies = lie;
	}

	public boolean getListenAttribute() {
		return willListen;
	}

	public int getlazyCoefficient() {
		return lazyCoefficient;
	}

	public void setLazyCoefficient(int coeff) {
		this.lazyCoefficient = coeff;
	}

	public void setListenAttribute(boolean listen) {
		this.willListen = listen;
	}

	public String getColor() {
		return color;
	}

	public String locationToString() {
		return "(" + String.valueOf(x) + ", " + String.valueOf(y) + ")";
	}

	public void setVelX(int velX) {
		this.velX = velX;
	}

	public void setVelY(int velY) {
		this.velY = velY;
	}

	public ArrayList<Node> getTargets() {
		return targets;
	}

	public void addTarget(Node target) {
		targets.add(target);
		numFound = targets.size();
	}

	public void addPath(Coordinate c) {
		path.add(c);
	}

	public Message getBroadcast() {
		return broadcast;
	}

	public void clearBroadcast() {
		broadcast = null;
	}

	public void addMessage(Message m) {
		inbox.add(m);
	}
}
