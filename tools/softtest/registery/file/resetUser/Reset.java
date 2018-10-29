package softtest.registery.file.resetUser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import softtest.registery.file.RegConstants;
import sun.misc.BASE64Decoder;

import ca.beq.util.win32.registry.RegistryKey;
import ca.beq.util.win32.registry.RootKey;

public class Reset implements ActionListener
{
	private ResetFrame rFrame;

	private String req;

	private String local;

	public Reset()
	{
		rFrame = new ResetFrame();
		rFrame.addActionLisener(this);
		createReq();
		rFrame.setReqField(req);
		rFrame.setVisible(true);
	}

	public void actionPerformed(ActionEvent event)
	{
		String response = rFrame.getRespondField();
		createLocal();
		if (response.equalsIgnoreCase(local))
		{
			work();
			JOptionPane.showMessageDialog(null, "           ���óɹ���", "SUCCESS",
					JOptionPane.PLAIN_MESSAGE);
		}
		else
		{
			JOptionPane.showMessageDialog(rFrame, "       ��Ӧ�벻�Ϸ���", "ERROR!",
					JOptionPane.ERROR_MESSAGE);
		}
		System.exit(0);
	}

	private void createReq()
	{
		long t = System.currentTimeMillis();
		req = Long.toHexString(t).toUpperCase();
	}

	private void createLocal()
	{
		try
		{
			byte[] data = (new BASE64Decoder()).decodeBuffer(req);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < data.length; i++)
			{
				sb.append(Integer.toHexString(data[i] & 0xFF));
			}
			local = sb.toString().toUpperCase();
		}
		catch (IOException ex)
		{
			local = null;
		}
	}

	private void work()
	{

		RegistryKey dtsKey = new RegistryKey(RootKey.HKEY_CURRENT_USER,
				RegConstants.LICENSE_REG_KEY);
		if (dtsKey.exists() && dtsKey.hasValue(RegConstants.LICENSE_REG_VALUE))
			dtsKey.deleteValue(RegConstants.LICENSE_REG_VALUE);

		File license = new File(RegConstants.LICENSE_FILE_PATH_0,
				RegConstants.LICENSE_FILE_NAME_0);
		if (license.exists())
			license.delete();

		File fBackup = new File(RegConstants.LICENSE_FILE_PATH_1,
				RegConstants.LICENSE_FILE_NAME_1);
		if (fBackup.exists())
			fBackup.delete();

	}

	public static void main(String[] args)
	{
		new Reset();
	}
}