import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;


public class Cliente extends JFrame{
	
	private JTextField usuarioTxt;
	private JTextArea chatCompleto;
	private ObjectOutputStream salida;
	private ObjectInputStream entrada;
	private String mensaje;
	private String IP;
	private Socket conexion;
	
	private JButton cerrar;
	
	private BigInteger[] kPubR;
	
	private BigInteger kPriv;
	
	private BigInteger[] kPub;
	
	public Cliente(String direccion) {
		super("Chat Encriptado [Cliente]");
		this.mensaje="";
		this.IP=direccion;
		this.setSize(400,400);
		this.setLayout(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.getContentPane().setBackground(new Color(217,237,214));
		//Titulos
		JLabel lbNombre = new JLabel("Milton Eduardo Barroso Ramírez. A01634505.");
		lbNombre.setBounds(10, -12, 300, 50);
		lbNombre.setFont(new Font("Arial",Font.ITALIC,12));
		this.add(lbNombre);
		
		JLabel titulo1=new JLabel("Chat");
		titulo1.setFont(new Font("Aharoni",Font.BOLD,50));
		titulo1.setForeground(new Color(97,211,71));
		titulo1.setBounds(30,-5,450,100);
		this.add(titulo1);
		
		JLabel titulo2=new JLabel("Encriptado");
		titulo2.setFont(new Font("Aharoni",Font.BOLD,50));
		titulo2.setForeground(new Color(97,211,71));
		titulo2.setBounds(80,31,550,100);
		this.add(titulo2);
		
		this.cerrar = new JButton("X");
		this.cerrar.setBounds(340, 110, 25, 25);
		this.cerrar.setBackground(new Color(255,222,222));
		this.cerrar.setBorder(new LineBorder(Color.BLACK, 1));
		this.cerrar.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					BigInteger[] tmp=Servidor.encriptar(kPubR[0], kPubR[1], "Cliente ha salido");
					salida.writeObject(tmp);
					mostrarMensaje("\nCliente ha salido");
					permitirEscritura(false);
				} catch (IOException ex) {
					chatCompleto.append("\nError al enviar el mensaje");
				}
			}
		});
		cerrar.setEnabled(false);
		this.add(this.cerrar);
		
		//Funcional
		this.usuarioTxt=new JTextField();
		this.usuarioTxt.setEditable(false);//No se puede escribir si no está nadie conectado
		this.usuarioTxt.addActionListener(
				new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						enviarMensaje(e.getActionCommand());
						usuarioTxt.setText("");
						
					}
				}
		);
		
		this.usuarioTxt.setBounds(16,310,350,25);
		this.add(usuarioTxt);
		this.chatCompleto=new JTextArea();
		//this.chatCompleto.setBounds(0,20,400,300);
		this.chatCompleto.setEditable(false);
		JScrollPane scroll=new JScrollPane(chatCompleto);
		scroll.setBounds(16,140,350,160);
		this.add(scroll);
		this.setVisible(true);
		
		this.generarLlaves();
	}
	
	private void generarLlaves() {
		
		this.kPub=new BigInteger[2];
		
		int pTmp,qTmp;
		
		do {
//			pTmp=(int) Math.floor(Math.random()*(2000-1000+1)+1000);
			pTmp=(int) Math.floor(Math.random()*(500-127+1)+127);
		} while (!Servidor.esPrimo(pTmp));
		do {
//			qTmp=(int) Math.floor(Math.random()*(2000-1000+1)+1000);
			qTmp=(int) Math.floor(Math.random()*(500-127+1)+127);
		} while (!Servidor.esPrimo(qTmp));
		
		BigInteger p = BigInteger.valueOf(pTmp);
		BigInteger q = BigInteger.valueOf(qTmp);
//		p=BigInteger.valueOf(131);
//		q=BigInteger.valueOf(137);
		System.out.println("p: "+p+" q: "+q);
		//System.out.println(BigInteger.valueOf(97).pow(57));
		
		BigInteger phi=q.subtract(BigInteger.valueOf(1)).multiply(p.subtract(BigInteger.valueOf(1)));
		System.out.println("phi: "+phi);
		
		BigInteger e;
		do {//Escoge un número que sea coprimo a phi desde 1
			e=new BigInteger(phi.bitLength(),10,new Random());
		} while (Servidor.mcd(e,phi).compareTo(BigInteger.ONE)!=0);
		
		BigInteger n=p.multiply(q);
		System.out.println("n: "+n);
		System.out.println("e: "+e);
		
		this.kPub[0]=n;
		this.kPub[1]=e;
		
		//Llave privada
		this.kPriv=Servidor.inversoM(e, phi);
		System.out.println("d: "+this.kPriv);
	}
	
	public void conectar() {
		
		try {
			
			this.conectarAServidor();
			this.prepararStreams();
			this.chateando();
			
		} catch (EOFException e) {
			this.mostrarMensaje("\nTerminaste la conexión");
		}catch (IOException e) {
			e.printStackTrace();
		}finally {
			this.cerrar();
		}
		
	}
	
	private void conectarAServidor() throws IOException{
		this.mostrarMensaje("Conectando...");
		this.conexion=new Socket(InetAddress.getByName(this.IP),5000);
		this.mostrarMensaje("\nConectado a "+conexion.getInetAddress().getHostName()+"!");
	}
	
	private void prepararStreams() throws IOException{
		this.salida=new ObjectOutputStream(this.conexion.getOutputStream());
		this.salida.flush();
		this.entrada=new ObjectInputStream(this.conexion.getInputStream());
		this.mostrarMensaje("\nLas conexiones están hechas!");
	}
	
	private void chateando() throws IOException{
		this.permitirEscritura(true);
		this.enviarLlavePub();
		BigInteger[] paquete;
		//Recibir llave pub
		try {
			this.kPubR=(BigInteger[]) this.entrada.readObject();
			System.out.println("Kpub recibida: "+kPubR[0]+", "+kPubR[1]);
		} catch (ClassNotFoundException e) {
			this.mostrarMensaje("\nError en la recepción de llaves");
		}
		
		do {
			try {
				paquete=(BigInteger[]) this.entrada.readObject();
				this.mensaje=Servidor.desencriptar(kPriv, kPub[0], paquete);;
				this.mostrarMensaje("\n"+this.mensaje);
			} catch (ClassNotFoundException e) {
				this.mostrarMensaje("\nError en la escritura");
			}
			
		} while (!this.mensaje.equals("Servidor ha salido"));
	}
	
	private void cerrar() {
		if(this.entrada==null) {
			JOptionPane.showMessageDialog(this, "No se encontró un servidor", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		this.mostrarMensaje("\nCerrando conexiones...");
		this.permitirEscritura(false);
		try {
			this.salida.close();
			this.entrada.close();
			this.conexion.close();
			JOptionPane.showMessageDialog(this, "Se ha terminado la conexión","Mensaje",JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void enviarLlavePub() {
		try {
			this.salida.writeObject(this.kPub);//Crea un objeto y lo manda a la otra instancia
			this.salida.flush();
			this.mostrarMensaje("\nLlave enviada");
		} catch (IOException e) {
			this.chatCompleto.append("\nError al enviar el mensaje");
		}
	}

	
	private void enviarMensaje(String mensaje) {
		try {
			BigInteger[] tmp=Servidor.encriptar(this.kPubR[0], this.kPubR[1], "Cliente: "+mensaje);
			this.salida.writeObject(tmp);
			this.salida.flush();
			this.mostrarMensaje("\nCliente: "+ mensaje);
		} catch (IOException e) {
			this.chatCompleto.append("\nError al enviar mensaje");
		}
	}
	
	private void mostrarMensaje(final String mensaje) {
		SwingUtilities.invokeLater(
				new Runnable() {
					
					@Override
					public void run() {
						chatCompleto.append(mensaje);
						
					}
				}
		);
	}
	
	private void permitirEscritura(boolean bool) {
		SwingUtilities.invokeLater(
				new Runnable() {
					
					@Override
					public void run() {
						usuarioTxt.setEditable(bool);
						cerrar.setEnabled(bool);
						
					}
				}
		);
		
		if(bool) {
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			this.cerrar.setBackground(new Color(255,108,108));
		}else {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.cerrar.setBackground(new Color(255,222,222));
		}
	}
	
	public static void main(String[] args) {
		Cliente prueba=new Cliente("127.0.0.1");
		prueba.conectar();
	}
	

}
