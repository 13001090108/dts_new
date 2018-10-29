package softtest.tools.c.viewer.gui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import softtest.tools.c.viewer.model.ViewerModel;
import softtest.tools.c.viewer.model.ViewerModelEvent;
import softtest.tools.c.viewer.model.ViewerModelListener;
import softtest.tools.c.viewer.util.NLS;


/**
 * �ܹ���קͼƬ�����
 */
public class ImagePanel extends JPanel implements ViewerModelListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3159579588700151603L;
	
	private ViewerModel model;
	
	private DragStatus status = DragStatus.Ready; // ��ק״̬
	private Image image; // Ҫ��ʾ��ͼƬ
	private Point imagePosition = new Point(0, 0), // ͼƬ�ĵ�ǰλ��
			imageStartposition = new Point(0, 0), // ÿ����ק��ʼʱͼƬ��λ�ã�Ҳ�����ϴ���ק���λ�ã�
			mouseStartposition; // ÿ����ק��ʼʱ����λ��

	private void init(String title) {
        model.addViewerModelListener(this);
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
        setLayout(new BorderLayout());
    }
	
	//	liuli	2010.4.13
	//��Ϊ����ͼ���������CG��壬����嶥����ʾ������title�������ͼ�Ĳ�ͬ�����Խ���ʾ������Ϊ��������
	public ImagePanel(ViewerModel m,String title) {
		this.model = m;
		addMouseListener(new MouseListener() {
			// ˫�����ʱ��ͼƬ
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					openImage();
				}
			}

			// �������ʱ������״̬�����Ҽ�¼��ק��ʼλ�á�
			public void mousePressed(MouseEvent e) {
				if (status == DragStatus.Ready) {
					status = DragStatus.Dragging;
					mouseStartposition = e.getPoint();
					imageStartposition.setLocation(imagePosition.getLocation());
				}
			}

			// �ɿ����ʱ����״̬
			public void mouseReleased(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					status = DragStatus.Ready;
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			// Java ����ק�¼���������¼����ƶ�ͼƬλ��
			public void mouseDragged(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					moveImage(e.getPoint());
				}
			}

			public void mouseMoved(MouseEvent e) {
			}
		});
		init(title);
	}
	
	public ImagePanel(String imagefile) {
		openImage(imagefile);
		
		addMouseListener(new MouseListener() {
			// ˫�����ʱ��ͼƬ
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					openImage();
				}
			}

			// �������ʱ������״̬�����Ҽ�¼��ק��ʼλ�á�
			public void mousePressed(MouseEvent e) {
				if (status == DragStatus.Ready) {
					status = DragStatus.Dragging;
					mouseStartposition = e.getPoint();
					imageStartposition.setLocation(imagePosition.getLocation());
				}
			}

			// �ɿ����ʱ����״̬
			public void mouseReleased(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					status = DragStatus.Ready;
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			// Java ����ק�¼���������¼����ƶ�ͼƬλ��
			public void mouseDragged(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					moveImage(e.getPoint());
				}
			}

			public void mouseMoved(MouseEvent e) {
			}
		});
	}
	
	public ImagePanel() {
		addMouseListener(new MouseListener() {
			// ˫�����ʱ��ͼƬ
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					openImage();
				}
			}

			// �������ʱ������״̬�����Ҽ�¼��ק��ʼλ�á�
			public void mousePressed(MouseEvent e) {
				if (status == DragStatus.Ready) {
					status = DragStatus.Dragging;
					mouseStartposition = e.getPoint();
					imageStartposition.setLocation(imagePosition.getLocation());
				}
			}

			// �ɿ����ʱ����״̬
			public void mouseReleased(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					status = DragStatus.Ready;
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			// Java ����ק�¼���������¼����ƶ�ͼƬλ��
			public void mouseDragged(MouseEvent e) {
				if (status == DragStatus.Dragging) {
					moveImage(e.getPoint());
				}
			}

			public void mouseMoved(MouseEvent e) {
			}
		});
	}

	/**
	 * �ƶ�ͼƬ��ʵ���ϻ�ͼ������ paintComponent() �н��У�����ֻ�Ǽ���ͼƬλ�ã�Ȼ����ø÷�����
	 * 
	 * @param point
	 *            ��ǰ�����λ��
	 */
	private void moveImage(Point point) {
		// ͼƬ�ĵ�ǰλ�õ���ͼƬ����ʼλ�ü������λ�õ�ƫ������
		imagePosition.setLocation(imageStartposition.getX()
				+ (point.getX() - mouseStartposition.getX()),
				imageStartposition.getY()
						+ (point.getY() - mouseStartposition.getY()));
		repaint();
	}

	public void openImage(String imagefile) {
		if (imagefile != null) {
			// image = null;
			// this.repaint();
			try {
				image = ImageIO.read(new File(imagefile));

				// image = Toolkit.getDefaultToolkit().getImage(imagefile);
				// image.flush();
				// image.setAccelerationPriority(1);
				// this.imageUpdate(img, infoflags, x, y, w, h)
				if (image != null) {
					repaint();
					//JTabbedPane jtp = ((JTabbedPane) (this.getParent()));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// ��ͼƬ
	private void openImage() {
		System.out.println("Opening image...");
		File file = createFileChooser().getSelectedFile();
		if (file != null) {
			image = Toolkit.getDefaultToolkit()
					.getImage(file.getAbsolutePath());
			if (image != null) {
				this.repaint();
			}
		}
	}

	// �������ļ��Ի���
	private JFileChooser createFileChooser() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("��ѡ��ͼƬ�ļ�...");
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("����ͼƬ��ʽ",
				"jpg", "jpeg", "gif", "png"));
		chooser.showOpenDialog(this);
		return chooser;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			g.drawImage(image, (int) imagePosition.getX(), (int) imagePosition
					.getY(), this);
		}
	}

	private enum DragStatus {
		Ready, Dragging
	}
	
	/**
     * @see ViewerModelListener#viewerModelChanged(ViewerModelEvent)
     */
    public void viewerModelChanged(ViewerModelEvent e) {
        switch (e.getReason()) {
        	case ViewerModelEvent.IMAGE_REPAINT:
        		//tree.setModel(new ASTModel(model.getRootNode()));
        		if (model.getImagePath() != null) {
        			System.out.println("OPEN : ".concat(model.getImagePath()));
        			this.openImage(model.getImagePath());
        		}
            break;
            //liuli 2010.4.13 ���������ʾ����ͼ
        	case ViewerModelEvent.IMAGE_CG_REPAINT:
        		if (model.getImagePath() != null) {
        			System.out.println("OPEN : ".concat(model.getImagePath()));
        			this.openImage(model.getImagePath());
        		}
            break;
        }
    }

    /**
     * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
     */
    /*
    public void valueChanged(TreeSelectionEvent e) {
        if (e.getNewLeadSelectionPath()!=null)
        model.selectNode((SimpleNode) e.getNewLeadSelectionPath().getLastPathComponent(), this);
    }
    */
}