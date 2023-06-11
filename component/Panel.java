package component;

import javax.swing.*;
import java.awt.*;

public class Panel extends JPanel{
    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    public Panel(){
        super();
        Dimension maxSize = getMaximumSize();
        if (maxSize != null){
            setSize(maxSize);
        }
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
        msgPanel.add(msgLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(msgPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

    }
}