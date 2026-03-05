import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

// --- EGYEDI OBJEKTUMOK ---

class MaintenanceBlock {
    int machineId; long start, end;
    public MaintenanceBlock(int m, long s, long e) { this.machineId = m; this.start = s; this.end = e; }
}

class MesResource {
    private int id; private long[][] setT;
    public MesResource(int id, int nr, int nj) { this.id = id; this.setT = new long[nj][nj]; }
    public long[][] getSetT() { return setT; }
    public void expandSetTMatrix(int newNj) {
        long[][] newSetT = new long[newNj][newNj];
        for (int i = 0; i < setT.length; i++) System.arraycopy(setT[i], 0, newSetT[i], 0, setT[i].length);
        this.setT = newSetT;
    }
    public void shrinkSetTMatrix(int delIdx, int newNj) {
        long[][] newSetT = new long[newNj][newNj];
        for (int i=0, ni=0; i<setT.length; i++) {
            if (i==delIdx) continue;
            for (int j=0, nj=0; j<setT[i].length; j++) {
                if (j==delIdx) continue;
                newSetT[ni][nj] = setT[i][j]; nj++;
            } ni++;
        } this.setT = newSetT;
    }
}

class MesJob {
    private int id; private String name; private boolean isVip; 
    private long[] procT, setupStartT, startT, endT;
    private long dueDate, releaseDate; private int weight;   

    public MesJob(int id, int nr) {
        this.id = id; this.name = "Ismeretlen"; this.isVip = false;
        this.procT = new long[nr]; this.setupStartT = new long[nr]; this.startT = new long[nr]; this.endT = new long[nr];
        this.weight = 1; this.dueDate = 1000; this.releaseDate = 0; 
    }
    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public boolean isVip() { return isVip; } public void setVip(boolean vip) { isVip = vip; }
    public long[] getProcT() { return procT; } public long[] getStartT() { return startT; }
    public long[] getSetupStartT() { return setupStartT; } public long[] getEndT() { return endT; }
    public long getDueDate() { return dueDate; } public void setDueDate(long d) { this.dueDate = d; }
    public long getReleaseDate() { return releaseDate; } public void setReleaseDate(long r) { this.releaseDate = r; }
    public int getWeight() { return weight; } public void setWeight(int w) { this.weight = w; }
}

class SimulationRecord {
    boolean visible; 
    int runId; String algorithm; long initScore, optScore, savedCost, cMax; double improvement;
    public SimulationRecord(int id, String alg, long is, long os, double imp, long cost, long cm) {
        this.visible = true; 
        this.runId = id; this.algorithm = alg; this.initScore = is; this.optScore = os; 
        this.improvement = imp; this.savedCost = cost; this.cMax = cm;
    }
}

// --- GRAFIKUS GANTT DIAGRAM ---
class GraphicalGanttPanel extends JPanel {
    private MesJob[] jobs; private int NJ, NR; private int[] schedule;
    private long cMax; private Color[] jobColors;
    private long currentTimeLine = -1; private ArrayList<MaintenanceBlock> maintenances;

    public GraphicalGanttPanel() { setBackground(new Color(245, 245, 250)); }

    public void updateChart(MesJob[] jobs, int NJ, int NR, int[] schedule, Color[] colors, ArrayList<MaintenanceBlock> maint) {
        this.jobs = jobs; this.NJ = NJ; this.NR = NR; this.schedule = schedule; this.jobColors = colors; this.maintenances = maint;
        if(jobs != null && schedule != null && NJ > 0 && NR > 0) this.cMax = jobs[schedule[NJ-1]].getEndT()[NR-1];
        repaint(); 
    }
    public void setTimeLine(long time) { this.currentTimeLine = time; repaint(); }
    public void clearChart() { this.jobs = null; this.schedule = null; this.currentTimeLine = -1; repaint(); }

    public void exportToPNG(Component parent) {
        if (jobs == null) { JOptionPane.showMessageDialog(parent, "Nincs mit menteni!"); return; }
        JFileChooser ch = new JFileChooser(); ch.setDialogTitle("Mentés Képként");
        if (ch.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = ch.getSelectedFile(); if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getAbsolutePath() + ".png");
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics(); paint(g2); g2.dispose();
            try { ImageIO.write(img, "png", f); JOptionPane.showMessageDialog(parent, "Sikeres mentés!"); } catch (Exception ex) {}
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (jobs == null || schedule == null || cMax <= 0 || NR <= 0) { drawPlaceholder(g); return; }

        Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int padX = 60, padY = 20, drawW = getWidth() - padX - 30, drawH = getHeight() - padY - 30;
        double scaleX = (double) drawW / cMax; int rowH = drawH / NR;

        g2.setColor(Color.LIGHT_GRAY);
        for(int r=0; r<=NR; r++) g2.drawLine(padX, padY+r*rowH, padX+drawW, padY+r*rowH);

        for (int r=0; r<NR; r++) {
            int y = padY + r * rowH;
            g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("Gép " + (r + 1), 10, y + rowH / 2 + 5);

            if (maintenances != null) {
                for (MaintenanceBlock mb : maintenances) {
                    if (mb.machineId == r) {
                        int mx = padX + (int)(mb.start * scaleX); int mw = Math.max(1, (int)((mb.end - mb.start) * scaleX));
                        g2.setColor(new Color(200, 50, 50, 120)); g2.fillRect(mx, y + 2, mw, rowH - 4);
                        g2.setColor(Color.RED); g2.drawRect(mx, y + 2, mw, rowH - 4);
                    }
                }
            }

            for (int i=0; i<NJ; i++) {
                int jobId = schedule[i];
                long sst = jobs[jobId].getSetupStartT()[r], st = jobs[jobId].getStartT()[r], et = jobs[jobId].getEndT()[r];
                int xSetup = padX + (int)(sst * scaleX), wSetup = Math.max(0, (int)((st - sst) * scaleX));
                int xJob = padX + (int)(st * scaleX), wJob = Math.max(2, (int)((et - st) * scaleX));
                int yJob = y + (int)(rowH * 0.2), hJob = (int)(rowH * 0.6);

                if (wSetup > 0) {
                    g2.setColor(Color.GRAY); g2.fillRect(xSetup, yJob, wSetup, hJob);
                    g2.setColor(Color.DARK_GRAY); g2.drawRect(xSetup, yJob, wSetup, hJob);
                }

                g2.setColor(jobColors[jobId]); g2.fillRoundRect(xJob, yJob, wJob, hJob, 6, 6);
                
                if (r == NR - 1) {
                    boolean isLate = jobs[jobId].getEndT()[NR-1] > jobs[jobId].getDueDate();
                    if(isLate) { g2.setColor(Color.RED); g2.setStroke(new BasicStroke(3)); } 
                    else { g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(1)); }
                    
                    g2.drawRoundRect(xJob, yJob, wJob, hJob, 6, 6); g2.setStroke(new BasicStroke(1));

                    int dueX = padX + (int)(jobs[jobId].getDueDate() * scaleX);
                    if (dueX <= padX + drawW + 40) { 
                        int yOffset = (i % 2 == 0) ? 25 : 42; 
                        g2.setColor(isLate ? Color.RED : Color.GRAY);
                        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
                        g2.drawLine(dueX, yJob - yOffset, dueX, yJob + hJob + 10);
                        g2.setStroke(new BasicStroke(1)); 
                        g2.setColor(jobColors[jobId]); g2.fillRect(dueX, yJob - yOffset, 24, 14);
                        g2.setColor(isLate ? Color.RED : Color.BLACK); g2.drawRect(dueX, yJob - yOffset, 24, 14);
                        g2.setColor(Color.BLACK); g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                        g2.drawString("J" + jobId, dueX + 4, yJob - yOffset + 11);
                        g2.setFont(new Font("SansSerif", Font.BOLD, 12)); 
                    }
                } else {
                    g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(1)); g2.drawRoundRect(xJob, yJob, wJob, hJob, 6, 6);
                }
                
                String txt = "J" + jobId; FontMetrics fm = g2.getFontMetrics();
                if (fm.stringWidth(txt) < wJob - 4) { g2.setColor(Color.BLACK); g2.drawString(txt, xJob + (wJob - fm.stringWidth(txt)) / 2, yJob + hJob / 2 + 5); }
            }
        }
        g2.setColor(Color.BLACK); g2.drawString("Cmax: " + cMax, padX + drawW - 30, padY + drawH + 25);
        if (currentTimeLine >= 0 && currentTimeLine <= cMax) {
            int lineX = padX + (int)(currentTimeLine * scaleX);
            g2.setColor(new Color(231, 76, 60, 200)); g2.setStroke(new BasicStroke(3));
            g2.drawLine(lineX, padY - 10, lineX, padY + drawH + 10);
        }
    }
    private void drawPlaceholder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.LIGHT_GRAY); g2.setFont(new Font("SansSerif", Font.ITALIC, 16));
        String msg = "A vizuális Gantt diagram a szimuláció futtatása után jelenik meg.";
        g2.drawString(msg, (getWidth() - g2.getFontMetrics().stringWidth(msg)) / 2, getHeight() / 2);
    }
}

// --- OKOS TREND GRAFIKON PANEL ---
class TrendChartPanel extends JPanel {
    private ArrayList<SimulationRecord> history;
    
    public boolean showInit = true, showOpt = true, showCmax = false, showImprov = false, showCost = false;
    
    public final Color C_INIT = new Color(231, 76, 60);
    public final Color C_OPT = new Color(46, 204, 113);
    public final Color C_CMAX = new Color(155, 89, 182);
    public final Color C_IMPROV = new Color(52, 152, 219);
    public final Color C_COST = new Color(241, 196, 15);

    public TrendChartPanel() { setBackground(Color.WHITE); }
    
    public void updateData(ArrayList<SimulationRecord> h) { this.history = h; revalidate(); repaint(); }
    
    private ArrayList<SimulationRecord> getActiveRecords() {
        ArrayList<SimulationRecord> list = new ArrayList<>();
        if(history != null) {
            for(SimulationRecord r : history) {
                if(r.visible) list.add(r);
            }
        }
        return list;
    }

    @Override
    public Dimension getPreferredSize() {
        int minSpacing = 80; int pad = 80;
        ArrayList<SimulationRecord> activeRecs = getActiveRecords();
        int reqW = 2 * pad + (Math.max(0, activeRecs.size() - 1) * minSpacing);
        Container parent = getParent();
        int parentW = (parent != null) ? parent.getWidth() : 800;
        return new Dimension(Math.max(parentW, reqW), 280); 
    }

    private void drawTrendLine(Graphics2D g2, int i, double v1, double v2, double maxVal, Color c, int pad, int w, int h_area, double scaleX) {
        if (maxVal <= 0) maxVal = 1; 
        int x1 = pad + (int)(i * scaleX), x2 = pad + (int)((i+1) * scaleX);
        int y1 = pad + h_area - (int)((v1 * h_area) / maxVal);
        int y2 = pad + h_area - (int)((v2 * h_area) / maxVal);
        g2.setColor(c); g2.drawLine(x1, y1, x2, y2);
        g2.fillOval(x1-4, y1-4, 8, 8); g2.fillOval(x2-4, y2-4, 8, 8);
    }

    private void drawSinglePoint(Graphics2D g2, double v, double maxVal, Color c, int x, int pad, int h_area) {
        if (maxVal <= 0) maxVal = 1;
        int y = pad + h_area - (int)((v * h_area) / maxVal);
        g2.setColor(c); g2.fillOval(x-4, y-4, 8, 8);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int viewX = (getParent() instanceof JViewport) ? ((JViewport)getParent()).getViewPosition().x : 0;
        ArrayList<SimulationRecord> activeRecs = getActiveRecords();

        if (activeRecs.isEmpty() || (!showInit && !showOpt && !showCmax && !showImprov && !showCost)) { 
            g2.setColor(Color.GRAY); g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g2.drawString("Nincs megjeleníthető adat. Pipálj be szűrőket felül, és ID-ket a táblázatban!", viewX + 50, 100); 
            return; 
        }

        int pad = 80, w = getWidth() - 2*pad, h_area = getHeight() - 2*pad;
        
        double maxPoints = 0.001, maxPct = 0.001, maxCostVal = 0.001; 
        for (SimulationRecord r : activeRecs) { 
            if(showInit && r.initScore > maxPoints) maxPoints = r.initScore; 
            if(showOpt && r.optScore > maxPoints) maxPoints = r.optScore; 
            if(showCmax && r.cMax > maxPoints) maxPoints = r.cMax;
            if(showImprov && r.improvement > maxPct) maxPct = r.improvement;
            if(showCost && r.savedCost > maxCostVal) maxCostVal = r.savedCost;
        }
        maxPoints *= 1.1; maxPct *= 1.1; maxCostVal *= 1.1;

        boolean hasPoints = showInit || showOpt || showCmax;
        boolean hasPct = showImprov;
        boolean hasCost = showCost;
        
        int activeGroups = (hasPoints ? 1 : 0) + (hasPct ? 1 : 0) + (hasCost ? 1 : 0);
        boolean mixedMode = activeGroups > 1;

        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        
        for(int i=0; i<=4; i++) {
            int y = pad + h_area - (i * h_area / 4);
            g2.drawLine(pad, y, pad + w, y);
            g2.setColor(Color.DARK_GRAY);
            
            String label = "";
            if (mixedMode) {
                label = (i * 25) + " % (Relatív)";
            } else if (hasPoints) {
                label = String.format("%,d", (long)((maxPoints / 4) * i));
            } else if (hasPct) {
                label = String.format("%.1f %%", (maxPct / 4) * i);
            } else if (hasCost) {
                label = String.format("%,d Ft", (long)((maxCostVal / 4) * i));
            }
            
            g2.drawString(label, viewX + 5, y + 5); 
            g2.setColor(Color.LIGHT_GRAY);
        }

        int legX = viewX + 80, legY = 15;
        g2.setColor(new Color(255, 255, 255, 230)); g2.fillRect(legX - 5, legY - 10, w - 10, 35); 
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        
        int cx = legX;
        if(showInit) { g2.setColor(C_INIT); g2.drawString(String.format("Kezdeti [Max: %,d]", (long)(maxPoints/1.1)), cx, legY); cx += 150; }
        if(showOpt) { g2.setColor(C_OPT); g2.drawString(String.format("Opt. [Max: %,d]", (long)(maxPoints/1.1)), cx, legY); cx += 130; }
        if(showCmax) { g2.setColor(C_CMAX); g2.drawString(String.format("Cmax [Max: %,d]", (long)(maxPoints/1.1)), cx, legY); cx += 130; }
        if(showImprov) { g2.setColor(C_IMPROV); g2.drawString(String.format("Javulás [Max: %.1f%%]", (maxPct/1.1)), cx, legY); cx += 150; }
        if(showCost) { g2.setColor(C_COST); g2.drawString(String.format("Megtakarítás [Max: %,d Ft]", (long)(maxCostVal/1.1)), cx, legY); cx += 180; }
        g2.setColor(Color.DARK_GRAY); g2.drawString("■ X: Futás ID", cx, legY);

        int n = activeRecs.size();
        g2.setStroke(new BasicStroke(2));

        if (n == 1) {
            int x = pad + w/2;
            SimulationRecord r = activeRecs.get(0);
            if(showInit) drawSinglePoint(g2, r.initScore, mixedMode ? maxPoints : maxPoints, C_INIT, x, pad, h_area);
            if(showOpt) drawSinglePoint(g2, r.optScore, mixedMode ? maxPoints : maxPoints, C_OPT, x, pad, h_area);
            if(showCmax) drawSinglePoint(g2, r.cMax, mixedMode ? maxPoints : maxPoints, C_CMAX, x, pad, h_area);
            if(showImprov) drawSinglePoint(g2, r.improvement, mixedMode ? maxPct : maxPct, C_IMPROV, x, pad, h_area);
            if(showCost) drawSinglePoint(g2, r.savedCost, mixedMode ? maxCostVal : maxCostVal, C_COST, x, pad, h_area);
            g2.setColor(Color.BLACK); g2.drawString("#" + r.runId, x - 10, pad + h_area + 20);
        } else if (n > 1) {
            double scaleX = (double)w / (n - 1);
            for (int i=0; i<n-1; i++) {
                SimulationRecord r1 = activeRecs.get(i), r2 = activeRecs.get(i+1);
                if(showInit) drawTrendLine(g2, i, r1.initScore, r2.initScore, maxPoints, C_INIT, pad, w, h_area, scaleX);
                if(showOpt) drawTrendLine(g2, i, r1.optScore, r2.optScore, maxPoints, C_OPT, pad, w, h_area, scaleX);
                if(showCmax) drawTrendLine(g2, i, r1.cMax, r2.cMax, maxPoints, C_CMAX, pad, w, h_area, scaleX);
                if(showImprov) drawTrendLine(g2, i, r1.improvement, r2.improvement, maxPct, C_IMPROV, pad, w, h_area, scaleX);
                if(showCost) drawTrendLine(g2, i, r1.savedCost, r2.savedCost, maxCostVal, C_COST, pad, w, h_area, scaleX);
            }
            g2.setColor(Color.BLACK);
            for (int i=0; i<n; i++) {
                int x = pad + (int)(i * scaleX);
                g2.drawString("#" + activeRecs.get(i).runId, x - 10, pad + h_area + 20);
            }
        }
        
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.BLACK);
        g2.drawLine(pad, pad, pad, pad + h_area); 
        g2.drawLine(pad, pad + h_area, pad + w, pad + h_area); 
    }
}

// --- OEE STATISZTIKA PANEL ---
class OEEPanel extends JPanel {
    private MesJob[] jobs; private int NJ, NR; private long cMax;
    public OEEPanel() { setBackground(Color.WHITE); }
    public void updateStats(MesJob[] jobs, int NJ, int NR, long cMax) {
        this.jobs = jobs; this.NJ = NJ; this.NR = NR; this.cMax = cMax; repaint();
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (jobs == null || cMax <= 0) { g.drawString("Futtasd le a szimulációt az OEE statisztikákhoz!", 50, 50); return; }
        Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(Color.BLACK); g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Jelmagyarázat:", 20, 25);
        g2.setColor(new Color(46, 204, 113)); g2.fillRect(140, 10, 20, 20); g2.setColor(Color.BLACK); g2.drawString("Hasznos Gyártás", 165, 25);
        g2.setColor(Color.GRAY); g2.fillRect(300, 10, 20, 20); g2.setColor(Color.BLACK); g2.drawString("Átállási Idő", 325, 25);
        g2.setColor(new Color(236, 240, 241)); g2.fillRect(430, 10, 20, 20); g2.setColor(Color.BLACK); g2.drawRect(430, 10, 20, 20); g2.drawString("Üresjárat / Karbantartás", 455, 25);

        int padX = 70, padY = 70, barW = getWidth() - padX - 180, rowH = (getHeight() - padY - 50) / NR;
        double maxUtil = 0; int bottleneck = 0;

        for (int r = 0; r < NR; r++) {
            long sumProc = 0, sumSetup = 0;
            for (int i=0; i<NJ; i++) { sumProc += jobs[i].getProcT()[r]; sumSetup += (jobs[i].getStartT()[r] - jobs[i].getSetupStartT()[r]); }
            int wP = (int)((double)sumProc/cMax * barW), wS = (int)((double)sumSetup/cMax * barW), wI = barW - wP - wS;
            
            double util = ((double)(sumProc + sumSetup) / cMax) * 100;
            if (util > maxUtil) { maxUtil = util; bottleneck = r + 1; }

            int y = padY + r * rowH;
            g2.setColor(Color.BLACK); g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Gép " + (r+1), 10, y + 25);

            g2.setColor(new Color(46, 204, 113)); g2.fillRect(padX, y, wP, 40); 
            g2.setColor(Color.GRAY); g2.fillRect(padX+wP, y, wS, 40); 
            g2.setColor(new Color(236, 240, 241)); g2.fillRect(padX+wP+wS, y, wI, 40); 
            g2.setColor(Color.BLACK); g2.drawRect(padX, y, barW, 40);
            
            g2.drawString(String.format("Hasznos: %.1f%%", (double)sumProc/cMax*100), padX + barW + 15, y + 25);
        }

        g2.setColor(new Color(192, 57, 43)); g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString(String.format("⚠️ SZŰK KERESZTMETSZET DETEKTÁLVA: Gép %d (Teljes terheltség: %.1f%%)", bottleneck, maxUtil), 20, getHeight() - 20);
    }
}

// --- FŐOSZTÁLY ---

public class FlowShopGUI extends JFrame {

    private int NJ = 8, NR = 4;
    private MesResource[] resources;
    private MesJob[] jobs;
    private ArrayList<MaintenanceBlock> maintenances = new ArrayList<>();
    
    private ArrayList<SimulationRecord> historyList = new ArrayList<>();
    private int runCounter = 1;
    private boolean isUpdatingTable = false; 

    // --- ÚJ: ÜZLETI MODELL VÁLTOZÓK ---
    private long machineCostRate = 1500; 
    private long penaltyCostRate = 5000; 
    private JTextField txtMachineCost, txtPenaltyCost;

    private Random rand = new Random();
    private final String ADMIN_PASSWORD = "admin";
    private final String[] CAR_BRANDS = {"BMW", "AUDI", "Mercedes-benz", "Nissan", "Alfa Romeo", "Fiat", "Lancia", "Maserati", "Peugeot", "Renault", "NSU"};

    private Color[] jobColors;
    private boolean[] activeJobs; 
    
    private JTextField txtNJ, txtNR, txtStep, txtLoop;
    private JComboBox<String> cmbAlgorithm;
    private DefaultTableModel jobTableModel, matrixTableModel, maintTableModel, historyTableModel;
    private JTable jobTable, matrixTable, maintTable, historyTable;
    private JTextArea txtConsole;
    
    private GraphicalGanttPanel visualGanttPanel;
    private OEEPanel oeePanel;
    private TrendChartPanel trendChartPanel;
    private JLabel lblInitScore, lblOptScore, lblImprov, lblSavedCost;
    private JSlider timeSlider; private Timer animationTimer;
    private long currentCmax = 0;

    public FlowShopGUI() {
        setTitle("Interaktív MES - Ipar 4.0 Szakdolgozat Mester Verzió");
        setSize(1300, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Vezérlőpult (Dashboard)", createControlPanel());
        tabbedPane.addTab("Termékek & Karbantartás", createProductionPanel()); 
        tabbedPane.addTab("Gépkihasználtság (OEE)", oeePanel = new OEEPanel());
        tabbedPane.addTab("Trendek & Történet", createTrendPanel()); 
        
        // --- ÚJ FÜL HOZZÁADÁSA ---
        tabbedPane.addTab("Üzleti Modell (Költségek)", createFinancialPanel());
        
        tabbedPane.addTab("Részletes Eredmények (Log)", createConsolePanel()); 

        add(tabbedPane, BorderLayout.CENTER);
        generateNewData(NJ, NR);
    }

    private void generateColors() {
        jobColors = new Color[NJ]; activeJobs = new boolean[NJ];
        for(int i=0; i<NJ; i++) jobColors[i] = Color.getHSBColor((i * 0.618f) % 1.0f, 0.5f, 0.95f);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel topWrapper = new JPanel(new BorderLayout());
        JPanel paramsContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel params = new JPanel(new GridLayout(2, 6, 10, 10)); 
        params.setBorder(BorderFactory.createTitledBorder("Alap Paraméterek & Algoritmus"));
        
        params.add(new JLabel("Munkák (NJ):")); txtNJ = new JTextField(String.valueOf(NJ), 5); params.add(txtNJ);
        params.add(new JLabel("Gépek (NR):")); txtNR = new JTextField(String.valueOf(NR), 5); params.add(txtNR);
        params.add(new JLabel("Algoritmus:")); cmbAlgorithm = new JComboBox<>(new String[]{"Lokális Kereső", "Kezdeti (Ad-hoc)"}); params.add(cmbAlgorithm);
        params.add(new JLabel("Lépések (STEP):")); txtStep = new JTextField("50", 5); params.add(txtStep);
        params.add(new JLabel("Szomszédok:")); txtLoop = new JTextField("20", 5); params.add(txtLoop);
        params.add(new JLabel("")); params.add(new JLabel("")); 
        paramsContainer.add(params);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnGen = new JButton("Új Adatok"), btnLoad = new JButton("TXT Import"), btnSave = new JButton("TXT Export");
        JButton btnCsvIn = new JButton("CSV Import"), btnCsvOut = new JButton("CSV Export");
        JButton btnRun = new JButton("Szimuláció Indítása ▶");
        btnRun.setBackground(new Color(60, 140, 220)); btnRun.setForeground(Color.WHITE); btnRun.setFont(new Font("Arial", Font.BOLD, 14));

        btnGen.addActionListener(e -> { NJ = Integer.parseInt(txtNJ.getText()); NR = Integer.parseInt(txtNR.getText()); maintenances.clear(); refreshMaintTable(); generateNewData(NJ, NR); });
        btnRun.addActionListener(e -> runSimulation());
        btnLoad.addActionListener(e -> loadFullState()); btnSave.addActionListener(e -> saveFullState());
        btnCsvOut.addActionListener(e -> exportToCSV()); btnCsvIn.addActionListener(e -> importFromCSV());

        buttons.add(btnGen); buttons.add(btnLoad); buttons.add(btnSave); buttons.add(btnCsvIn); buttons.add(btnCsvOut); buttons.add(btnRun);
        topWrapper.add(paramsContainer, BorderLayout.NORTH); topWrapper.add(buttons, BorderLayout.CENTER);
        panel.add(topWrapper, BorderLayout.NORTH);

        JPanel matrixPanel = new JPanel(new BorderLayout());
        matrixPanel.setBorder(BorderFactory.createTitledBorder("Műveleti idők mátrixa (ProcT)"));
        matrixTableModel = new DefaultTableModel() { @Override public boolean isCellEditable(int r, int c) { return false; } };
        matrixTable = new JTable(matrixTableModel); matrixTable.getTableHeader().setBackground(new Color(230, 230, 230));
        
        matrixTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                try {
                    String rowVal = t.getModel().getValueAt(r, 0).toString();
                    int jId = Integer.parseInt(rowVal.substring(rowVal.indexOf("J") + 1, rowVal.indexOf(" -"))); 
                    if(activeJobs != null && jId < activeJobs.length && activeJobs[jId]) comp.setBackground(jobColors[jId]);
                    else comp.setBackground(Color.WHITE);
                } catch(Exception ex) { comp.setBackground(Color.WHITE); } return comp;
            }
        });
        matrixPanel.add(new JScrollPane(matrixTable), BorderLayout.CENTER);

        JPanel ganttWrapper = new JPanel(new BorderLayout());
        ganttWrapper.setBorder(BorderFactory.createTitledBorder("Vizuális Gantt Diagram"));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExpPNG = new JButton("📷 PNG Kép Mentése"); btnExpPNG.addActionListener(e -> visualGanttPanel.exportToPNG(this));
        toolbar.add(btnExpPNG); ganttWrapper.add(toolbar, BorderLayout.NORTH);

        visualGanttPanel = new GraphicalGanttPanel(); ganttWrapper.add(visualGanttPanel, BorderLayout.CENTER);

        JPanel animPanel = new JPanel(new BorderLayout());
        JButton btnPlay = new JButton("▶ Lejátszás"); timeSlider = new JSlider(0, 100, 0); timeSlider.setEnabled(false);
        animationTimer = new Timer(50, e -> {
            int val = timeSlider.getValue() + 2;
            if (val > timeSlider.getMaximum()) { animationTimer.stop(); btnPlay.setText("▶ Lejátszás"); } 
            else timeSlider.setValue(val);
        });
        btnPlay.addActionListener(e -> {
            if(currentCmax<=0) return;
            if(animationTimer.isRunning()) { animationTimer.stop(); btnPlay.setText("▶ Lejátszás"); }
            else { if(timeSlider.getValue() >= timeSlider.getMaximum()) timeSlider.setValue(0); animationTimer.start(); btnPlay.setText("⏸ Szünet"); }
        });
        timeSlider.addChangeListener(e -> {
            if (currentCmax > 0) {
                long t = timeSlider.getValue(); visualGanttPanel.setTimeLine(t); Arrays.fill(activeJobs, false);
                for (int i=0; i<NJ; i++) for (int r=0; r<NR; r++) if (t>=jobs[i].getStartT()[r] && t<jobs[i].getEndT()[r]) activeJobs[i]=true;
                matrixTable.repaint();
            }
        });
        animPanel.add(btnPlay, BorderLayout.WEST); animPanel.add(timeSlider, BorderLayout.CENTER);
        ganttWrapper.add(animPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, matrixPanel, ganttWrapper);
        splitPane.setResizeWeight(0.35); splitPane.setDividerSize(6);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel db = new JPanel(new GridLayout(1, 4, 15, 0)); db.setPreferredSize(new Dimension(0, 90));
        lblInitScore = createScoreCard("Kezdeti Büntetőpont", "-", new Color(231, 76, 60)); 
        lblOptScore = createScoreCard("Optimalizált Eredmény", "-", new Color(46, 204, 113)); 
        lblImprov = createScoreCard("Javulás (%)", "-", new Color(52, 152, 219)); 
        lblSavedCost = createScoreCard("Megtakarított Költség", "-", new Color(241, 196, 15)); 
        
        db.add(lblInitScore); db.add(lblOptScore); db.add(lblImprov); db.add(lblSavedCost); 
        panel.add(db, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createScoreCard(String t, String v, Color c) {
        JLabel l = new JLabel(); l.setHorizontalAlignment(SwingConstants.CENTER); l.setOpaque(true);
        l.setBackground(new Color(45, 45, 48)); l.setBorder(new LineBorder(c, 3, true)); 
        String h = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        l.setText("<html><div style='text-align: center;'><span style='color:#CCC; font-size:11px;'>" + t + "</span><br><span style='font-size:24px; color:" + h + ";'><b>" + v + "</b></span></div></html>");
        return l;
    }

    // --- ÚJ: ÜZLETI MODELL (PÉNZÜGY) FÜL ---
    private JPanel createFinancialPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        formPanel.setBorder(BorderFactory.createTitledBorder("Fajlagos Költség Paraméterek"));
        
        formPanel.add(new JLabel("Gépüzemeltetés fajlagos költsége (Ft / időegység / gép):"));
        txtMachineCost = new JTextField(String.valueOf(machineCostRate));
        txtMachineCost.setFont(new Font("Arial", Font.BOLD, 14));
        formPanel.add(txtMachineCost);
        
        formPanel.add(new JLabel("Súlyozott késedelmi kötbér (Ft / időegység / súly):"));
        txtPenaltyCost = new JTextField(String.valueOf(penaltyCostRate));
        txtPenaltyCost.setFont(new Font("Arial", Font.BOLD, 14));
        formPanel.add(txtPenaltyCost);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(formPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        JButton btnApply = new JButton("Változtatások Alkalmazása");
        JButton btnSaveModel = new JButton("Üzleti Modell Mentése");
        JButton btnLoadModel = new JButton("Üzleti Modell Betöltése");

        btnApply.setBackground(new Color(46, 204, 113)); btnApply.setForeground(Color.WHITE);
        btnApply.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnApply.addActionListener(e -> applyFinancialModel());
        btnSaveModel.addActionListener(e -> saveFinancialModel());
        btnLoadModel.addActionListener(e -> loadFinancialModel());

        btnPanel.add(btnApply); btnPanel.add(btnSaveModel); btnPanel.add(btnLoadModel);
        wrapper.add(btnPanel, BorderLayout.CENTER);

        JTextArea info = new JTextArea(
            "Információ az Üzleti Modellről:\n\n" +
            "Ez a modul teszi lehetővé a szimuláció pénzügyi finomhangolását anélkül, hogy a kódot módosítani kellene.\n" +
            "- Gépüzemeltetési költség: Mennyibe kerül a vállalatnak, amíg a gépek futnak (rezsi, áram, operátorok bére).\n" +
            "- Késedelmi kötbér: A határidő túllépése esetén a megrendelő felé fizetendő büntetés, a termék fontosságával (súlyával) szorozva.\n\n" +
            "Elmenthetsz különböző gazdasági szcenáriókat (pl. 'energiaválság_modell.txt', 'szigorú_ügyfél_modell.txt') és később gombnyomással betöltheted őket."
        );
        info.setEditable(false);
        info.setBackground(panel.getBackground());
        info.setFont(new Font("SansSerif", Font.PLAIN, 14));
        info.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        wrapper.add(info, BorderLayout.SOUTH);

        panel.add(wrapper, BorderLayout.NORTH);
        return panel;
    }

    private void applyFinancialModel() {
        try {
            long newMachine = Long.parseLong(txtMachineCost.getText().trim());
            long newPenalty = Long.parseLong(txtPenaltyCost.getText().trim());
            if (newMachine < 0 || newPenalty < 0) throw new NumberFormatException();
            
            machineCostRate = newMachine;
            penaltyCostRate = newPenalty;
            JOptionPane.showMessageDialog(this, "Pénzügyi paraméterek sikeresen frissítve!\nA következő szimuláció már ezekkel fog számolni.", "Siker", JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Kérlek érvényes, pozitív egész számokat adj meg a költségekhez!", "Hiba", JOptionPane.ERROR_MESSAGE);
            txtMachineCost.setText(String.valueOf(machineCostRate));
            txtPenaltyCost.setText(String.valueOf(penaltyCostRate));
        }
    }

    private void saveFinancialModel() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Üzleti Modell Mentése");
        if(ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
            File f = ch.getSelectedFile();
            if(!f.getName().endsWith(".txt")) f = new File(f.getAbsolutePath() + ".txt");
            try(PrintWriter w = new PrintWriter(f)){
                w.println("MACHINE_COST=" + txtMachineCost.getText().trim());
                w.println("PENALTY_COST=" + txtPenaltyCost.getText().trim());
                JOptionPane.showMessageDialog(this, "Üzleti modell sikeresen elmentve!");
            }catch(Exception e){ JOptionPane.showMessageDialog(this, "Hiba a mentés során!"); }
        }
    }

    private void loadFinancialModel() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Üzleti Modell Betöltése");
        if(ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            try(Scanner sc = new Scanner(ch.getSelectedFile())){
                while(sc.hasNextLine()){
                    String line = sc.nextLine();
                    if(line.startsWith("MACHINE_COST=")) txtMachineCost.setText(line.split("=")[1]);
                    if(line.startsWith("PENALTY_COST=")) txtPenaltyCost.setText(line.split("=")[1]);
                }
                applyFinancialModel(); // Automatikusan érvényesíti a betöltött számokat
            }catch(Exception e){ JOptionPane.showMessageDialog(this, "Hiba a betöltés során vagy hibás fájlformátum!"); }
        }
    }

    private JPanel createProductionPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10)); p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        JPanel pJobs = new JPanel(new BorderLayout());
        pJobs.setBorder(BorderFactory.createTitledBorder("Gyártandó Termékek"));
        jobTableModel = new DefaultTableModel(new String[]{"ID", "Név (Márka)", "Beérkezés (R)", "Határidő (D)", "Súly", "Műveleti idők"}, 0){ @Override public boolean isCellEditable(int r, int c) { return false; } };
        jobTable = new JTable(jobTableModel); jobTable.setRowHeight(25); 
        pJobs.add(new JScrollPane(jobTable), BorderLayout.CENTER);
        
        JPanel bP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddJob = new JButton("+ Új Termék"); btnAddJob.setBackground(new Color(40, 160, 80)); btnAddJob.setForeground(Color.WHITE);
        JButton btnEditJob = new JButton("✎ Módosítás"); 
        JButton btnDelJob = new JButton("✖ Törlés"); btnDelJob.setBackground(new Color(200, 50, 50)); btnDelJob.setForeground(Color.WHITE);
        
        btnAddJob.addActionListener(e -> addNewJobInteractively());
        btnAddJob.addActionListener(e -> editSelectedJobInteractively());
        btnAddJob.addActionListener(e -> deleteSelectedJobInteractively());
        
        bP.add(btnAddJob); bP.add(btnEditJob); bP.add(btnDelJob); pJobs.add(bP, BorderLayout.SOUTH); 
        
        JPanel pMaint = new JPanel(new BorderLayout());
        pMaint.setBorder(BorderFactory.createTitledBorder("Aktív Karbantartások (Leállások)"));
        maintTableModel = new DefaultTableModel(new String[]{"Gép", "Kezdete (T)", "Vége (T)"}, 0){ @Override public boolean isCellEditable(int r, int c) { return false; } };
        maintTable = new JTable(maintTableModel); maintTable.setRowHeight(25);
        pMaint.add(new JScrollPane(maintTable), BorderLayout.CENTER);

        JPanel bPMaint = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddMaint = new JButton("+ Új Karbantartás"); btnAddMaint.setBackground(new Color(40, 160, 80)); btnAddMaint.setForeground(Color.WHITE);
        JButton btnEditMaint = new JButton("✎ Módosítás");
        JButton btnDelMaint = new JButton("✖ Törlés"); btnDelMaint.setBackground(new Color(200, 50, 50)); btnDelMaint.setForeground(Color.WHITE);
        JButton btnClearMaint = new JButton("Összes Törlése"); 

        btnAddMaint.addActionListener(e -> addMaintenanceInteractively());
        btnAddMaint.addActionListener(e -> editMaintenanceInteractively());
        btnAddMaint.addActionListener(e -> deleteMaintenanceInteractively());
        btnClearMaint.addActionListener(e -> { 
            if(!verifyAdmin()) return; 
            maintenances.clear(); refreshMaintTable(); JOptionPane.showMessageDialog(this, "Minden karbantartás törölve!"); 
        });

        bPMaint.add(btnAddMaint); bPMaint.add(btnEditMaint); bPMaint.add(btnDelMaint); bPMaint.add(btnClearMaint); 
        pMaint.add(bPMaint, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pJobs, pMaint);
        split.setResizeWeight(0.5); split.setDividerLocation(300); p.add(split, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel createTrendPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSaveHistory = new JButton("Eredmények Mentése (CSV)");
        JButton btnLoadHistory = new JButton("Eredmények Betöltése (CSV)");
        JButton btnClearHistory = new JButton("Előzmények Törlése");
        
        btnSaveHistory.addActionListener(e -> saveHistoryCSV());
        btnLoadHistory.addActionListener(e -> loadHistoryCSV());
        btnClearHistory.addActionListener(e -> { historyList.clear(); runCounter = 1; refreshHistoryView(); });
        toolbar.add(btnSaveHistory); toolbar.add(btnLoadHistory); toolbar.add(btnClearHistory);

        trendChartPanel = new TrendChartPanel();
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Megjelenített metrikák (Több kijelölése esetén az Y-tengely relatív %-ra vált)"));
        
        JCheckBox chkAll = new JCheckBox("ÖSSZES"); chkAll.setFont(new Font("Arial", Font.BOLD, 12));
        JCheckBox chkInit = new JCheckBox("Kezdeti Pontszám", true); chkInit.setForeground(trendChartPanel.C_INIT); 
        JCheckBox chkOpt = new JCheckBox("Opt. Pontszám", true); chkOpt.setForeground(trendChartPanel.C_OPT); 
        JCheckBox chkCmax = new JCheckBox("Cmax (Teljes idő)", false); chkCmax.setForeground(trendChartPanel.C_CMAX); 
        JCheckBox chkImprov = new JCheckBox("Javulás (%)", false); chkImprov.setForeground(trendChartPanel.C_IMPROV); 
        JCheckBox chkCost = new JCheckBox("Megtakarítás (Ft)", false); chkCost.setForeground(trendChartPanel.C_COST); 

        chkAll.addItemListener(e -> {
            boolean sel = chkAll.isSelected();
            chkInit.setSelected(sel); chkOpt.setSelected(sel); chkCmax.setSelected(sel); chkImprov.setSelected(sel); chkCost.setSelected(sel);
        });

        chkInit.addItemListener(e -> { trendChartPanel.showInit = chkInit.isSelected(); trendChartPanel.repaint(); });
        chkOpt.addItemListener(e -> { trendChartPanel.showOpt = chkOpt.isSelected(); trendChartPanel.repaint(); });
        chkCmax.addItemListener(e -> { trendChartPanel.showCmax = chkCmax.isSelected(); trendChartPanel.repaint(); });
        chkImprov.addItemListener(e -> { trendChartPanel.showImprov = chkImprov.isSelected(); trendChartPanel.repaint(); });
        chkCost.addItemListener(e -> { trendChartPanel.showCost = chkCost.isSelected(); trendChartPanel.repaint(); });

        filterPanel.add(chkAll);
        filterPanel.add(new JLabel(" | "));
        filterPanel.add(chkInit); filterPanel.add(chkOpt); filterPanel.add(chkCmax); filterPanel.add(chkImprov); filterPanel.add(chkCost);

        JScrollPane chartScroll = new JScrollPane(trendChartPanel);
        chartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 
        chartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); 
        
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.add(filterPanel, BorderLayout.NORTH);
        chartWrapper.add(chartScroll, BorderLayout.CENTER);
        
        historyTableModel = new DefaultTableModel(new String[]{"Látható", "Futás ID", "Algoritmus", "Kezdeti Pont", "Opt. Pont", "Cmax", "Javulás (%)", "Megtakarítás (Ft)"}, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) { return columnIndex == 0 ? Boolean.class : Object.class; }
            @Override public boolean isCellEditable(int row, int column) { return column == 0; } 
        };
        
        historyTable = new JTable(historyTableModel); historyTable.setRowHeight(25);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(80);

        historyTable.getColumnModel().getColumn(0).setHeaderRenderer(new TableCellRenderer() {
            JCheckBox cb = new JCheckBox("Látható");
            {
                cb.setHorizontalAlignment(SwingConstants.CENTER);
                cb.setBackground(UIManager.getColor("TableHeader.background"));
                cb.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                cb.setFont(new Font("SansSerif", Font.BOLD, 11));
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                boolean allTrue = true;
                if (table.getModel().getRowCount() == 0) {
                    allTrue = false;
                } else {
                    for(int i=0; i<table.getModel().getRowCount(); i++) {
                        if(!(Boolean)table.getModel().getValueAt(i, 0)) {
                            allTrue = false; break;
                        }
                    }
                }
                cb.setSelected(allTrue);
                return cb;
            }
        });

        historyTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = historyTable.columnAtPoint(e.getPoint());
                if (col == 0) { 
                    boolean allTrue = true;
                    for(int i=0; i<historyTable.getModel().getRowCount(); i++) {
                        if(!(Boolean)historyTable.getModel().getValueAt(i, 0)) {
                            allTrue = false; break;
                        }
                    }
                    boolean newState = !allTrue; 
                    
                    for (SimulationRecord rec : historyList) {
                        rec.visible = newState;
                    }
                    refreshHistoryView(); 
                }
            }
        });

        historyTableModel.addTableModelListener(e -> {
            if (!isUpdatingTable && e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                int row = e.getFirstRow();
                if (row >= 0 && row < historyList.size()) {
                    boolean isChecked = (Boolean) historyTableModel.getValueAt(row, 0);
                    historyList.get(row).visible = isChecked;
                    trendChartPanel.updateData(historyList); 
                    historyTable.getTableHeader().repaint(); 
                }
            }
        });
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Részletes Napló"));
        tablePanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.add(toolbar, BorderLayout.NORTH);
        
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartWrapper, tablePanel);
        split.setResizeWeight(0.65);
        mainContent.add(split, BorderLayout.CENTER);
        
        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void refreshHistoryView() {
        isUpdatingTable = true; 
        historyTableModel.setRowCount(0);
        for(SimulationRecord r : historyList) {
            historyTableModel.addRow(new Object[]{r.visible, "#" + r.runId, r.algorithm, r.initScore, r.optScore, r.cMax, String.format(java.util.Locale.US, "%.2f", r.improvement), r.savedCost});
        }
        isUpdatingTable = false; 
        trendChartPanel.updateData(historyList);
        if (historyTable != null && historyTable.getTableHeader() != null) {
            historyTable.getTableHeader().repaint(); 
        }
    }

    private void saveHistoryCSV() {
        if (historyList.isEmpty()) { JOptionPane.showMessageDialog(this, "Nincs mit menteni!"); return; }
        JFileChooser ch = new JFileChooser(); ch.setDialogTitle("Eredmények Mentése");
        if(ch.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
            File f = ch.getSelectedFile(); if(!f.getName().endsWith(".csv")) f = new File(f.getAbsolutePath()+".csv");
            try(PrintWriter w = new PrintWriter(f)){
                w.println("RunID;Algoritmus;KezdetiPont;OptPont;Cmax;Javulas;Megtakaritas");
                for(SimulationRecord r : historyList) w.println(r.runId+";"+r.algorithm+";"+r.initScore+";"+r.optScore+";"+r.cMax+";"+r.improvement+";"+r.savedCost);
                JOptionPane.showMessageDialog(this, "Történet mentve!");
            }catch(Exception e){}
        }
    }

    private void loadHistoryCSV() {
        JFileChooser ch = new JFileChooser();
        if(ch.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
            try(Scanner sc = new Scanner(ch.getSelectedFile())){
                sc.nextLine(); historyList.clear(); runCounter = 1;
                while(sc.hasNextLine()){
                    String[] p = sc.nextLine().split(";"); if(p.length < 7) continue;
                    int id = Integer.parseInt(p[0]);
                    historyList.add(new SimulationRecord(id, p[1], Long.parseLong(p[2]), Long.parseLong(p[3]), Double.parseDouble(p[5].replace(",",".")), Long.parseLong(p[6]), Long.parseLong(p[4])));
                    if(id >= runCounter) runCounter = id + 1;
                }
                refreshHistoryView();
                JOptionPane.showMessageDialog(this, "Történet betöltve!");
            }catch(Exception e){ JOptionPane.showMessageDialog(this, "Hiba a beolvasáskor!"); }
        }
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        txtConsole = new JTextArea(); txtConsole.setEditable(false);
        txtConsole.setBackground(new Color(30, 30, 30)); txtConsole.setForeground(new Color(200, 200, 200)); 
        txtConsole.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
        panel.add(new JScrollPane(txtConsole), BorderLayout.CENTER);
        return panel;
    }

    private boolean verifyAdmin() {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, pf, "Admin jelszó szükséges (admin):", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
            if (new String(pf.getPassword()).equals(ADMIN_PASSWORD)) return true;
            JOptionPane.showMessageDialog(this, "Helytelen jelszó!", "Hiba", JOptionPane.ERROR_MESSAGE);
        } return false;
    }

    private void addMaintenanceInteractively() {
        if (!verifyAdmin()) return;
        JTextField mID = new JTextField("1"), st = new JTextField("100"), et = new JTextField("200");
        if(JOptionPane.showConfirmDialog(this, new Object[]{"Gép sorszáma (1-"+NR+"):", mID, "Kezdete (T):", st, "Vége (T):", et}, "Új Karbantartás", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
            try {
                int machineIdx = Integer.parseInt(mID.getText()) - 1;
                long startT = Long.parseLong(st.getText()); long endT = Long.parseLong(et.getText());
                if (machineIdx < 0 || machineIdx >= NR) throw new Exception("Érvénytelen gép sorszám!");
                if (startT < 0 || endT < 0) throw new Exception("Az időpontok nem lehetnek negatívak!");
                if (endT <= startT) throw new Exception("A befejezési időnek nagyobbnak kell lennie a kezdésnél!");
                maintenances.add(new MaintenanceBlock(machineIdx, startT, endT)); refreshMaintTable();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void editMaintenanceInteractively() {
        int r = maintTable.getSelectedRow(); if (r == -1) { JOptionPane.showMessageDialog(this, "Jelölj ki egy sort a módosításhoz!"); return; }
        if (!verifyAdmin()) return;
        MaintenanceBlock mb = maintenances.get(r);
        JTextField mID = new JTextField(String.valueOf(mb.machineId + 1)), st = new JTextField(String.valueOf(mb.start)), et = new JTextField(String.valueOf(mb.end));
        if(JOptionPane.showConfirmDialog(this, new Object[]{"Gép sorszáma (1-"+NR+"):", mID, "Kezdete (T):", st, "Vége (T):", et}, "Karbantartás Módosítása", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
            try {
                int machineIdx = Integer.parseInt(mID.getText()) - 1;
                long startT = Long.parseLong(st.getText()); long endT = Long.parseLong(et.getText());
                if (machineIdx < 0 || machineIdx >= NR) throw new Exception("Érvénytelen gép sorszám!");
                if (startT < 0 || endT < 0) throw new Exception("Az időpontok nem lehetnek negatívak!");
                if (endT <= startT) throw new Exception("A befejezési időnek nagyobbnak kell lennie a kezdésnél!");
                mb.machineId = machineIdx; mb.start = startT; mb.end = endT; refreshMaintTable();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void deleteMaintenanceInteractively() {
        int r = maintTable.getSelectedRow(); if (r == -1) { JOptionPane.showMessageDialog(this, "Jelölj ki egy sort a törléshez!"); return; }
        if (!verifyAdmin()) return;
        if (JOptionPane.showConfirmDialog(this, "Biztosan törlöd a karbantartást?", "Megerősítés", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            maintenances.remove(r); refreshMaintTable();
        }
    }

    private void refreshMaintTable() {
        maintTableModel.setRowCount(0);
        for(MaintenanceBlock mb : maintenances) maintTableModel.addRow(new Object[]{"Gép " + (mb.machineId + 1), mb.start, mb.end});
    }

    private void addNewJobInteractively() {
        if (!verifyAdmin()) return;
        JComboBox<String> cmbBrand = new JComboBox<>(CAR_BRANDS);
        JCheckBox chkVip = new JCheckBox("Sürgős (VIP)");
        JTextField tW = new JTextField("3"), tD = new JTextField("1000");
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Márka (Név):", cmbBrand, chkVip, "Súly (Prioritás):", tW, "Határidő:", tD}, "Új Termék", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int newW = Integer.parseInt(tW.getText()); long newD = Long.parseLong(tD.getText());
                if (newW <= 0) throw new Exception("A súlynak pozitív számnak kell lennie!");
                if (newD < 0) throw new Exception("A határidő nem lehet negatív!");

                int oldNJ = NJ; NJ++; generateColors();
                MesJob[] newJobs = new MesJob[NJ]; System.arraycopy(jobs, 0, newJobs, 0, oldNJ);
                MesJob addedJob = new MesJob(oldNJ, NR); addedJob.setName((String) cmbBrand.getSelectedItem()); addedJob.setVip(chkVip.isSelected()); addedJob.setWeight(newW); addedJob.setDueDate(newD);
                for(int r=0; r<NR; r++) addedJob.getProcT()[r] = 10+rand.nextInt(90);
                newJobs[oldNJ] = addedJob; this.jobs = newJobs;
                for (MesResource res : resources) { res.expandSetTMatrix(NJ); for (int i=0; i<NJ; i++) { res.getSetT()[i][oldNJ] = (i==oldNJ)?0:10+rand.nextInt(50); res.getSetT()[oldNJ][i] = (i==oldNJ)?0:10+rand.nextInt(50); } }
                txtNJ.setText(String.valueOf(NJ)); refreshTables(); visualGanttPanel.clearChart();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void editSelectedJobInteractively() {
        int r = jobTable.getSelectedRow(); if (r == -1) { JOptionPane.showMessageDialog(this, "Jelölj ki egy sort a módosításhoz!"); return; }
        if (!verifyAdmin()) return; 
        
        MesJob sJob = jobs[r];
        JComboBox<String> cmbBrand = new JComboBox<>(CAR_BRANDS); cmbBrand.setSelectedItem(sJob.getName());
        JCheckBox chkVip = new JCheckBox("Sürgős (VIP)", sJob.isVip());
        JTextField tW = new JTextField(String.valueOf(sJob.getWeight())), tD = new JTextField(String.valueOf(sJob.getDueDate()));
        
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Márka (Név):", cmbBrand, chkVip, "Súly (Prioritás):", tW, "Határidő:", tD}, "Módosítás", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int newW = Integer.parseInt(tW.getText()); long newD = Long.parseLong(tD.getText());
                if (newW <= 0) throw new Exception("A súlynak pozitív számnak kell lennie!");
                if (newD < 0) throw new Exception("A határidő nem lehet negatív!");
                
                sJob.setName((String) cmbBrand.getSelectedItem()); sJob.setVip(chkVip.isSelected()); sJob.setWeight(newW); sJob.setDueDate(newD);
                refreshTables(); visualGanttPanel.clearChart();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void deleteSelectedJobInteractively() {
        int r = jobTable.getSelectedRow(); if (r == -1) { JOptionPane.showMessageDialog(this, "Jelölj ki egy sort a törléshez!"); return; }
        if(NJ <= 1) { JOptionPane.showMessageDialog(this, "Legalább 1 termék kell!"); return; }
        if (!verifyAdmin()) return; 
        if (JOptionPane.showConfirmDialog(this, "Törlöd a " + r + ". terméket?", "Törlés", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            NJ--; generateColors();
            MesJob[] newJobs = new MesJob[NJ];
            for (int i=0, newI=0; i < jobs.length; i++) { if (i == r) continue; jobs[i].setId(newI); newJobs[newI++] = jobs[i]; }
            this.jobs = newJobs;
            for (MesResource res : resources) res.shrinkSetTMatrix(r, NJ);
            txtNJ.setText(String.valueOf(NJ)); refreshTables(); visualGanttPanel.clearChart();
        }
    }

    private void generateNewData(int nj, int nr) {
        this.NJ = nj; this.NR = nr; generateColors();
        resources = new MesResource[NR];
        for (int r=0; r<NR; r++) { resources[r] = new MesResource(r, NR, NJ); for(int i=0; i<NJ; i++) for(int j=0; j<NJ; j++) resources[r].getSetT()[i][j] = (i==j)?0:10+rand.nextInt(30); }
        jobs = new MesJob[NJ];
        for (int i=0; i<NJ; i++) { 
            jobs[i] = new MesJob(i, NR); 
            jobs[i].setName(CAR_BRANDS[rand.nextInt(CAR_BRANDS.length)]); 
            jobs[i].setVip(rand.nextInt(100) > 85); 
            jobs[i].setWeight(1+rand.nextInt(5)); jobs[i].setDueDate(300+rand.nextInt(800)); jobs[i].setReleaseDate(rand.nextInt(100));
            for (int r=0; r<NR; r++) jobs[i].getProcT()[r] = 10+rand.nextInt(60); 
        }
        refreshTables();
    }

    private void refreshTables() {
        jobTableModel.setRowCount(0); 
        for(MesJob j:jobs) {
            String dispName = (j.isVip() ? "⭐ " : "") + j.getName();
            jobTableModel.addRow(new Object[]{j.getId(), dispName, j.getReleaseDate(), j.getDueDate(), j.getWeight(), Arrays.toString(j.getProcT())});
        }
        String[] c = new String[NR+1]; c[0]="Termék ID"; for(int r=0; r<NR; r++) c[r+1] = (r+1)+". Gép";
        matrixTableModel.setColumnIdentifiers(c); matrixTableModel.setRowCount(0);
        for(MesJob j:jobs) { 
            Object[] r = new Object[NR+1]; r[0]="J"+j.getId()+" - "+ (j.isVip()?"⭐ ":"") + j.getName(); 
            for(int i=0; i<NR; i++) r[i+1]=j.getProcT()[i]; matrixTableModel.addRow(r); 
        }
    }

    private void exportToCSV() {
        JFileChooser ch = new JFileChooser();
        if(ch.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
            File f = ch.getSelectedFile(); if(!f.getName().endsWith(".csv")) f = new File(f.getAbsolutePath()+".csv");
            try(PrintWriter w = new PrintWriter(f)){
                w.println("ID;Name;VIP;ReleaseDate;DueDate;Weight;ProcT_1_to_NR");
                for(MesJob j:jobs) { w.print(j.getId()+";"+j.getName()+";"+j.isVip()+";"+j.getReleaseDate()+";"+j.getDueDate()+";"+j.getWeight()+";"); for(long p:j.getProcT()) w.print(p+";"); w.println(); }
                JOptionPane.showMessageDialog(this, "CSV sikeresen mentve!");
            }catch(Exception e){}
        }
    }

    private void importFromCSV() {
        JFileChooser ch = new JFileChooser();
        if(ch.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
            try(Scanner sc = new Scanner(ch.getSelectedFile())){
                sc.nextLine(); 
                ArrayList<MesJob> jList = new ArrayList<>(); int nrCount = 0;
                while(sc.hasNextLine()){
                    String[] parts = sc.nextLine().split(";"); if(parts.length < 7) continue;
                    nrCount = parts.length - 6; MesJob j = new MesJob(Integer.parseInt(parts[0]), nrCount);
                    j.setName(parts[1]); j.setVip(Boolean.parseBoolean(parts[2])); j.setReleaseDate(Long.parseLong(parts[3])); j.setDueDate(Long.parseLong(parts[4])); j.setWeight(Integer.parseInt(parts[5]));
                    for(int x=0; x<nrCount; x++) j.getProcT()[x] = Long.parseLong(parts[6+x]); jList.add(j);
                }
                this.NJ = jList.size(); this.NR = nrCount; this.jobs = jList.toArray(new MesJob[0]);
                txtNJ.setText(NJ+""); txtNR.setText(NR+""); generateColors(); refreshTables();
                JOptionPane.showMessageDialog(this, "CSV Sikeresen betöltve!");
            }catch(Exception e){ JOptionPane.showMessageDialog(this, "Hiba a beolvasáskor!"); }
        }
    }

    private void saveFullState() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter out = new PrintWriter(chooser.getSelectedFile())) {
                out.println(NJ + " " + NR); out.println("---JOBS---");
                for (MesJob j : jobs) { out.print(j.getId() + " " + j.getName() + " " + j.isVip() + " " + j.getWeight() + " " + j.getDueDate() + " "); for(long p : j.getProcT()) out.print(p + " "); out.println(); }
                JOptionPane.showMessageDialog(this, "Mentve!");
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void loadFullState() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (Scanner sc = new Scanner(chooser.getSelectedFile())) {
                NJ = sc.nextInt(); NR = sc.nextInt(); sc.next(); 
                generateColors(); jobs = new MesJob[NJ];
                for (int i=0; i<NJ; i++) {
                    jobs[i] = new MesJob(sc.nextInt(), NR); jobs[i].setName(sc.next()); jobs[i].setVip(sc.nextBoolean()); jobs[i].setWeight(sc.nextInt()); jobs[i].setDueDate(sc.nextLong());
                    for (int r=0; r<NR; r++) jobs[i].getProcT()[r] = sc.nextLong();
                }
                resources = new MesResource[NR];
                for (int r=0; r<NR; r++) { resources[r] = new MesResource(r, NR, NJ); for (int i=0; i<NJ; i++) for (int j=0; j<NJ; j++) resources[r].getSetT()[i][j] = (i==j)?0:10+rand.nextInt(50); }
                txtNJ.setText(String.valueOf(NJ)); txtNR.setText(String.valueOf(NR)); refreshTables(); visualGanttPanel.clearChart();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Hiba a fájl beolvasásakor!"); }
        }
    }

    private void simulationFS(MesJob[] job, int NJ, MesResource[] res, int NR, int[] s, long t0) {
        for (int i = 0; i < NJ; i++) {
            MesJob cJ = job[s[i]]; int cId = cJ.getId(); int pId = (i==0) ? cId : job[s[i-1]].getId();
            for (int r = 0; r < NR; r++) {
                long ready = (r==0) ? Math.max(t0, cJ.getReleaseDate()) : cJ.getEndT()[r-1];
                long mReady = (i==0) ? 0 : job[s[i-1]].getEndT()[r];
                long stSetup = Math.max(ready, mReady);
                long setTime = (i==0) ? 0 : res[r].getSetT()[pId][cId];
                long pTime = cJ.getProcT()[r];
                
                for(MaintenanceBlock mb : maintenances) {
                    if(mb.machineId == r && stSetup < mb.end && (stSetup + setTime + pTime) > mb.start) stSetup = mb.end;
                }
                cJ.getSetupStartT()[r] = stSetup; cJ.getStartT()[r] = stSetup + setTime; cJ.getEndT()[r] = cJ.getStartT()[r] + pTime;
            }
        }
    }

    // --- ÚJ: FRISSÍTETT CÉLFÜGGVÉNYEK A DINAMIKUS PÉNZÜGYEKKEL ---
    private long evaluate(MesJob[] job, int NJ, int NR, int[] s) {
        long cMax = job[s[NJ-1]].getEndT()[NR-1], pen = 0;
        for(int i=0; i<NJ; i++) {
            long tard = Math.max(0, job[i].getEndT()[NR-1] - job[i].getDueDate());
            long actWeight = job[i].isVip() ? (job[i].getWeight() * 1000) : job[i].getWeight(); 
            pen += (tard * actWeight);
        }
        return cMax + pen; 
    }

    private long calculateCost(MesJob[] job, int NJ, int NR, int[] s, long cMax) {
        // AZ ÚJ BEÁLLÍTOTT VÁLTOZÓKAT HASZNÁLJA A KONSTANSOK HELYETT!
        long machineCost = cMax * NR * machineCostRate; 
        long penaltyCost = 0;
        for(int i=0; i<NJ; i++) {
            long tard = Math.max(0, job[i].getEndT()[NR-1] - job[i].getDueDate());
            penaltyCost += (tard * job[i].getWeight() * penaltyCostRate); 
        }
        return machineCost + penaltyCost;
    }

    private String generateVisualGanttChartText(MesJob[] job, int NJ, int NR, int[] s) {
        StringBuilder sb = new StringBuilder(); sb.append("=== KONZOLOS (ASCII) GANTT DIAGRAM ===\n");
        long realCmax = job[s[NJ - 1]].getEndT()[NR - 1]; double scale = 100.0 / (realCmax > 0 ? realCmax : 1);
        for (int r = 0; r < NR; r++) {
            sb.append(String.format(" %2d. gép: |", r)); long currentTime = 0;
            for (int i = 0; i < NJ; i++) {
                int jobId = s[i]; long start = job[jobId].getStartT()[r]; long end = job[jobId].getEndT()[r];
                for (int c = 0; c < Math.round((start - currentTime) * scale); c++) sb.append(" ");
                int jobChars = Math.max(1, (int) Math.round((end - start) * scale));
                StringBuilder jobBlock = new StringBuilder("=".repeat(jobChars)); String idStr = String.valueOf(jobId);
                if (jobChars >= idStr.length()) { int mid = (jobChars - idStr.length()) / 2; jobBlock.replace(mid, mid + idStr.length(), idStr); }
                sb.append(jobBlock); currentTime = end;
            } sb.append("| (Bef: ").append(currentTime).append(")\n");
        } sb.append("=====================================\n\n"); return sb.toString();
    }

    private void runSimulation() {
        
        // Ellenőrizzük, hogy módosított-e valamit a pénzügyi fülön alkalmazás nélkül
        try {
            machineCostRate = Long.parseLong(txtMachineCost.getText().trim());
            penaltyCostRate = Long.parseLong(txtPenaltyCost.getText().trim());
        } catch(Exception ignored) {}

        if(animationTimer != null) animationTimer.stop(); Arrays.fill(activeJobs, false);
        int[] s = new int[NJ], sA = new int[NJ], sB = new int[NJ], s0 = new int[NJ], sE = new int[NJ];
        for(int i=0; i<NJ; i++) s[i]=i; 
        System.arraycopy(s,0,s0,0,NJ); System.arraycopy(s,0,sB,0,NJ);
        
        simulationFS(jobs, NJ, resources, NR, sB, 0);
        long initScore = evaluate(jobs, NJ, NR, sB);
        long cBest = initScore;
        long initCost = calculateCost(jobs, NJ, NR, sB, jobs[sB[NJ-1]].getEndT()[NR-1]);
        
        if (cmbAlgorithm.getSelectedIndex() == 0) {
            int STEP = Integer.parseInt(txtStep.getText()), LOOP = Integer.parseInt(txtLoop.getText());
            long cExt = Long.MAX_VALUE;
            
            for(int step=1; step<=STEP; step++) {
                cExt = Long.MAX_VALUE;
                for(int loop=1; loop<=LOOP; loop++) {
                    System.arraycopy(s0,0,sA,0,NJ); 
                    int x=rand.nextInt(NJ), y=rand.nextInt(NJ);
                    int temp = sA[x]; sA[x] = sA[y]; sA[y] = temp;
                    
                    simulationFS(jobs, NJ, resources, NR, sA, 0); 
                    long cAct = evaluate(jobs, NJ, NR, sA);
                    
                    if(loop==1 || cAct < cExt) { 
                        System.arraycopy(sA,0,sE,0,NJ);
                        cExt = cAct; 
                    }
                }
                System.arraycopy(sE,0,s0,0,NJ);
                if(cExt < cBest) { cBest = cExt; System.arraycopy(sE,0,sB,0,NJ); }
            }
        }
        
        System.arraycopy(sB,0,s,0,NJ); 
        simulationFS(jobs, NJ, resources, NR, s, 0);
        currentCmax = jobs[s[NJ-1]].getEndT()[NR-1];
        
        long finalCost = calculateCost(jobs, NJ, NR, s, currentCmax);
        long savedCost = initCost - finalCost;
        double pct = (initScore > 0) ? ((double)(initScore-cBest)/initScore)*100.0 : 0.0;

        String h = (cmbAlgorithm.getSelectedIndex()==0) ? String.format(java.util.Locale.US, "%.1f %%", pct) : "N/A";
        String sc = (cmbAlgorithm.getSelectedIndex()==0 && savedCost > 0) ? String.format("%,d Ft", savedCost) : "0 Ft";

        lblInitScore.setText("<html><center><span style='color:#ccc; font-size:11px;'>Kezdeti Büntetőpont</span><br><b style='font-size:24px;color:#E74C3C'>"+initScore+"</b></center></html>");
        lblOptScore.setText("<html><center><span style='color:#ccc; font-size:11px;'>Optimalizált</span><br><b style='font-size:24px;color:#2ECC71'>"+cBest+"</b></center></html>");
        lblImprov.setText("<html><center><span style='color:#ccc; font-size:11px;'>Javulás</span><br><b style='font-size:24px;color:#3498DB'>"+h+"</b></center></html>");
        lblSavedCost.setText("<html><center><span style='color:#ccc; font-size:11px;'>Megtakarított Költség</span><br><b style='font-size:24px;color:#F1C40F'>"+sc+"</b></center></html>");

        visualGanttPanel.updateChart(jobs, NJ, NR, s, jobColors, maintenances); oeePanel.updateStats(jobs, NJ, NR, currentCmax);
        timeSlider.setEnabled(true); timeSlider.setMaximum((int)currentCmax); timeSlider.setValue(0); visualGanttPanel.setTimeLine(0);

        historyList.add(new SimulationRecord(runCounter++, cmbAlgorithm.getSelectedItem().toString(), initScore, cBest, pct, savedCost, currentCmax));
        refreshHistoryView();

        txtConsole.setText("--- SZIMULÁCIÓ EREDMÉNYEK ---\n");
        txtConsole.append("Aktuális Pénzügyi Modell: Gép: " + machineCostRate + " Ft/T | Kötbér: " + penaltyCostRate + " Ft/T\n");
        txtConsole.append("Kezdeti Büntetőpont: " + initScore + "\n");
        txtConsole.append("Optimalizált Büntetőpont: " + cBest + " (Cmax + Súlyozott késés)\n\n");
        txtConsole.append(generateVisualGanttChartText(jobs, NJ, NR, s));
        txtConsole.append("--- KÉSÉSEK RÉSZLETEZÉSE (Kiszállítás a legutolsó gépről) ---\n");
        for (int jobId : s) {
            long endT = jobs[jobId].getEndT()[NR - 1]; long tard = Math.max(0, endT - jobs[jobId].getDueDate());
            String vipTag = jobs[jobId].isVip() ? "[VIP] " : "";
            txtConsole.append(String.format("%sJob %d (%s) | Bef: %4d | Határidő: %4d | Késés: %d (Súly: %d)\n", vipTag, jobId, jobs[jobId].getName(), endT, jobs[jobId].getDueDate(), tard, jobs[jobId].getWeight()));
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new FlowShopGUI().setVisible(true)); }
}