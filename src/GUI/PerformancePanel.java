package GUI;

import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.NetworkIF;
import oshi.util.FormatUtil;

public class PerformancePanel extends OshiJPanel{

    /*
     * Create protected static attributes for CPU, RAM, Disk, Network.
     * All subclass will inherit (extends) from this PerformancePanel class.
     */

    //protected static List<Long> diskReadSpeed = new ArrayList<Long>(100);
    //protected static List<Long> diskWriteSpeed = new ArrayList<Long>(100);
  
    protected static List<Long> diskReadSpeed = new ArrayList<>(
    Collections.nCopies(100, (long)0));
    protected static List<Long> diskWriteSpeed = new ArrayList<>(
    Collections.nCopies(100, (long)0));

    
    protected static void updateDiskInfo(List<HWDiskStore> diskStores, JGradientButton[] diskButton)
    {
        Thread thread = new Thread(() -> {
            while(true)
            {
                long timeNow[] = new long[diskStores.size()];
                long readLast[] = new long[diskStores.size()];
                long writeLast[] = new long[diskStores.size()];
                long readNow[] = new long[diskStores.size()];
                long writeNow[] = new long[diskStores.size()];
                for (int i = 0; i < diskStores.size() ; i++)
                {
                    HWDiskStore disk = diskStores.get(i);
                    timeNow[i] = disk.getTimeStamp();
                    readLast[i] = disk.getReadBytes();
                    writeLast[i] = disk.getWriteBytes();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                for (int i = 0; i < diskStores.size() ; i++)
                {
                    HWDiskStore disk = diskStores.get(i);
                    disk.updateAttributes();
                    readNow[i] = disk.getReadBytes();
                    writeNow[i] = disk.getWriteBytes();
                    diskReadSpeed.set(i, (readNow[i] - readLast[i])*1000/(disk.getTimeStamp()-timeNow[i]));
                    diskWriteSpeed.set(i, (writeNow[i] - writeLast[i])*1000/(disk.getTimeStamp()-timeNow[i]));
                    diskButton[i].setText(updateDisk(disk, i, diskReadSpeed.get(i), diskWriteSpeed.get(i)));
                }
            }
        });
        thread.start();
    }

    public PerformancePanel() {
    }

    public PerformancePanel(SystemInfo si) {
        super();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initial(si);
    }
    
    class JVerticalMenuBar extends JMenuBar {
        private static final LayoutManager grid = new GridLayout(0,1);
        public JVerticalMenuBar() {
            setLayout(grid);
        }
    }
    
    private static final class JGradientButton extends JButton{
        private JGradientButton(String text){
            super(text);
            setContentAreaFilled(false);
        }
        
        public Color color;

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setPaint(new GradientPaint(
                    new Point(0, 0), 
                    Color.WHITE, 
                    new Point(0, getHeight()), 
                    this.color));
            //g2.setPaint(color);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();

            super.paintComponent(g);
        }
    }

    private void initial(SystemInfo si) {
        JPanel perfPanel = new JPanel();
        perfPanel.setLayout(new GridBagLayout());

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new GridBagLayout());

        JPanel perfMenuBar = new JPanel();
        perfMenuBar.setLayout(new GridBagLayout());

        JGradientButton cpuButton = createButton(updateCPUString(si), 'C', "Display CPU", Color.GREEN, new CPUPanel(si), displayPanel);

        GridBagConstraints cpuC = new GridBagConstraints();
        cpuC.fill = GridBagConstraints.HORIZONTAL;
        cpuC.gridx = 0;
        cpuC.gridy = 0;
        perfMenuBar.add(cpuButton, cpuC);
        
        JGradientButton memButton = createButton(updateMemoryString(si), 'M', "Display Memory", Color.GREEN,new MemoryPanel(si), displayPanel);

        GridBagConstraints memC = (GridBagConstraints)cpuC.clone();
        memC.gridx = 0;
        memC.gridy = 1;
        perfMenuBar.add(memButton, memC);

        int y = 2;

        List<HWDiskStore> hwDiskStore = si.getHardware().getDiskStores();
        JGradientButton[] diskButton = new JGradientButton[hwDiskStore.size()];
        for (int i = 0; i < hwDiskStore.size() ; i++)
        {
            HWDiskStore disk = hwDiskStore.get(i);
            diskButton[i] = createDiskButton(updateDisk(disk, i, 0, 0), 'D', "Display Disk",Color.PINK.darker() , disk, i, displayPanel);
            GridBagConstraints diskC = (GridBagConstraints)cpuC.clone();
            diskC.gridy =  y;
            y++;
            perfMenuBar.add(diskButton[i], diskC);
        }

        updateDiskInfo(si.getHardware().getDiskStores(), diskButton);

        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs(false);
        JGradientButton[] netButton = new JGradientButton[networkIFs.size()];
        for (int i = 0; i < networkIFs.size() ; i++)
        {
            NetworkIF net = networkIFs.get(i);
            netButton[i] = createNetworkButton(updateNetwork(net, 0, 0), 'N', "Display Network",Color.CYAN.brighter() , net, displayPanel);
            GridBagConstraints netC = (GridBagConstraints)cpuC.clone();
            netC.gridy =  y;
            y++;
            perfMenuBar.add(netButton[i], netC);
        }

        GridBagConstraints perfMenuBarConstraints = new GridBagConstraints();
        perfMenuBarConstraints.gridx = 0;
        perfMenuBarConstraints.gridy = 0;
        perfMenuBarConstraints.weightx = 1d;
        perfMenuBarConstraints.weighty = 1d;
        perfMenuBarConstraints.anchor = GridBagConstraints.NORTHWEST;

        JScrollPane scrollPerfPanel = new JScrollPane(perfMenuBar);
        scrollPerfPanel.setSize(new Dimension(320, getSize().height));
        scrollPerfPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        perfPanel.add(scrollPerfPanel, perfMenuBarConstraints);
        
        GridBagConstraints perfConstraints = new GridBagConstraints();
        perfConstraints.gridx = 0;
        perfConstraints.gridy = 0;
        perfConstraints.weightx = 1d;
        perfConstraints.weighty = 1d;
        perfConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(perfPanel, perfConstraints);

        // Update up time every second
        Timer timer = new Timer(Config.REFRESH_FAST, e -> {
            
            Thread cpuThread = new Thread( ()->{
                updateCPU(si, cpuButton);
            });
            cpuThread.start();
    
            updateMemory(si,memButton);
        });
        timer.start();
        Thread thread = new Thread(() -> {
            while(true)
            {
                for (int i = 0; i < networkIFs.size() ; i++)
                {
                    NetworkIF net = networkIFs.get(i);
                    long timeNow = net.getTimeStamp();
                    long recvLast = net.getBytesRecv();
                    long sendLast = net.getBytesSent();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    net.updateAttributes();
                    long recvNow = net.getBytesRecv();
                    long sendNow = net.getBytesSent();
                    long sendSpeed = (sendNow - sendLast)*1000/(net.getTimeStamp()-timeNow);
                    long recvSpeed = (recvNow - recvLast)*1000/(net.getTimeStamp()-timeNow);
                    netButton[i].setText(updateNetwork(net, recvSpeed, sendSpeed));
                }
            }
        });
        thread.start();

        GridBagConstraints displayConstraints = new GridBagConstraints();
        displayConstraints.gridx = 1;
        displayConstraints.gridy = 0;
        displayConstraints.weightx = 1;
        displayConstraints.weighty = 1d;
        displayConstraints.fill = GridBagConstraints.NONE;
        displayConstraints.anchor = GridBagConstraints.NORTHWEST;

        perfPanel.add(displayPanel, displayConstraints);

        cpuButton.doClick();
    }

    public static String updateDisk(HWDiskStore disk, int index, long recvSpeed, long sendSpeed)
    {
        StringBuffer nameBuffer = new StringBuffer();
        nameBuffer.append("Disk " + String.valueOf(index) + " (");
        for (HWPartition partition: disk.getPartitions())
        {
            nameBuffer.append(partition.getMountPoint() + " ");
        }
        nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        nameBuffer.append(")");
        String name;
        if (nameBuffer.length() > 30) {
            name = nameBuffer.substring(0,30) + "...";
        }
        else{
            name = nameBuffer.toString();
        }
        String txt = name + "\nRead: " + FormatUtil.formatBytes(sendSpeed) + "\nWrite: " + FormatUtil.formatBytes(recvSpeed) + '\n';
        return buttonTextLines(txt);
    }

    public static String updateNetwork(NetworkIF net, long recvSpeed, long sendSpeed)
    {
        String name = net.getDisplayName();
        if (name.length() > 30)
        {
            name = name.substring(0,30) + "...";
        }
        String alias = net.getIfAlias();
        if (alias.length() > 30)
        {
            alias = alias.substring(0,30) + "...";
        }
        String txt = name + "\n" + alias + "\nSend: " + FormatUtil.formatBytes(sendSpeed) + "\nReceive: " + FormatUtil.formatBytes(recvSpeed);
        return buttonTextLines(txt);
    }

    private Color getColorByPercent(double percent)
    {
        if (percent < 60.d) {
            return Color.GREEN;
        }
        else if (percent < 80d) {
            return Color.YELLOW;
        }
        else {
            return Color.RED;
        }
    }

    private String updateMemoryString(SystemInfo si) {
        GlobalMemory mem = si.getHardware().getMemory();
        double available = (double)mem.getAvailable()/(1024*1024*1024);
        double total = (double)mem.getTotal()/(1024*1024*1024);
        double used = total - available;
        return buttonTextLines("\nMemory\n" + (String.format("%.2f/%.2f GB (%.0f)", used, total, (used/total)*100) + "%\n"));
    }

    private String updateCPUString(SystemInfo si)
    {
        double load = si.getHardware().getProcessor().getSystemCpuLoad(1000);
        return buttonTextLines("\nCPU\n" + (String.format("%.2f",load*100)) + "%\n");
    }

    private void updateMemory(SystemInfo si, JGradientButton memButton) {
        GlobalMemory mem = si.getHardware().getMemory();
        double available = (double)mem.getAvailable()/(1024*1024*1024);
        double total = (double)mem.getTotal()/(1024*1024*1024);
        double used = total - available;
        memButton.setText(buttonTextLines("\nMemory\n" + (String.format("%.2f/%.2f GB (%.0f)", used, total, (used/total)*100) + "%\n")));
        memButton.color = getColorByPercent((used/total)*100);
    }

    private void updateCPU(SystemInfo si, JGradientButton cpuButton)
    {
        double load = si.getHardware().getProcessor().getSystemCpuLoad(1000);
        cpuButton.setText(buttonTextLines("\nCPU\n" + (String.format("%.2f",load*100)) + "%\n"));
        cpuButton.color = getColorByPercent(load*100);
    }

    private JGradientButton createNetworkButton(String title, char mnemonic, String toolTip, Color color, NetworkIF net, JPanel displayPanel)
    {
        JGradientButton button = new JGradientButton(title);
        button.color = color;
        button.setFont(button.getFont().deriveFont(16f));
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        OshiJPanel panel = new NetworkPanel(net, button);
        // Set what to do when we push the button
        button.addActionListener(e -> {
            int nComponents = (int)displayPanel.getComponents().length;
            if (nComponents <= (int)0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                displayPanel.add(panel);
                refreshMainGui(displayPanel);
            }
        });
        return button;
    }

    private JGradientButton createDiskButton(String title, char mnemonic, String toolTip, Color color, HWDiskStore disk, int index, JPanel displayPanel)
    {
        JGradientButton button = new JGradientButton(title);
        button.color = color;
        button.setFont(button.getFont().deriveFont(16f));
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        OshiJPanel panel = new DiskPanel(disk, index);
        // Set what to do when we push the button
        button.addActionListener(e -> {
            int nComponents = (int)displayPanel.getComponents().length;
            if (nComponents <= (int)0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                displayPanel.add(panel);
                refreshMainGui(displayPanel);
            }
        });
        return button;
    }


    private JGradientButton createButton(String title, char mnemonic, String toolTip, Color color, OshiJPanel panel, JPanel displayPanel)
    {
        JGradientButton button = new JGradientButton(title);
        button.color = color;
        button.setFont(button.getFont().deriveFont(16f));
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        // Set what to do when we push the button
        button.addActionListener(e -> {
            int nComponents = (int)displayPanel.getComponents().length;
            if (nComponents <= (int)0 || displayPanel.getComponent(0) != panel) {
                resetMainGui(displayPanel);
                displayPanel.add(panel);
                refreshMainGui(displayPanel);
            }
        });
        return button;
    }
    
    private void resetMainGui(JPanel displayPanel) {
        displayPanel.removeAll();
    }

    private void refreshMainGui(JPanel displayPanel) {
        displayPanel.revalidate();
        displayPanel.repaint();
    }

    public static String buttonTextLines(String txt)
    {
        return "<html>" + htmlSpace(3) + txt.replaceAll("\n", "<br>" + htmlSpace(3));
    }

    public static String htmlSpace(int num)
    {
        return "&nbsp;".repeat(num);
    }
}
