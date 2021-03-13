import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;


public class Servidor extends JFrame{
	
	private JTextField usuarioTxt;
	private JTextArea chatCompleto;
	private ObjectOutputStream salida;
	private ObjectInputStream entrada;
	private ServerSocket servidor;
	private Socket conexion;
	
	private JButton cerrar;
	
	private BigInteger kPriv;
	
	private BigInteger[] kPub;
	
	private BigInteger[] kPubR;
	
	//Interfaz GUI
	public Servidor () {
		super("Chat Encriptado [Servidor]");
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
					BigInteger[] tmp=encriptar(kPubR[0], kPubR[1], "Servidor ha salido");
					salida.writeObject(tmp);
					salida.flush();
					mostrarMensaje("\nServidor ha salido");
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
		} while (!esPrimo(pTmp));
		do {
//			qTmp=(int) Math.floor(Math.random()*(2000-1000+1)+1000);
			qTmp=(int) Math.floor(Math.random()*(500-127+1)+127);
		} while (!esPrimo(qTmp));
		
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
		} while (mcd(e,phi).compareTo(BigInteger.ONE)!=0);
		
		BigInteger n=p.multiply(q);
		System.out.println("n: "+n);
		System.out.println("e: "+e);
		
		this.kPub[0]=n;
		this.kPub[1]=e;
		
		//Llave privada
		this.kPriv=inversoM(e, phi);
		System.out.println("d: "+this.kPriv);
		
	}
	
	public static BigInteger[] encriptar(BigInteger m,BigInteger e,String txt) {
		char[] msg=txt.toCharArray();
		BigInteger[] nums=new BigInteger[msg.length];
		System.out.print("Enviado: ");
		for (int i = 0; i < msg.length; i++) {
			nums[i]=BigInteger.valueOf(msg[i]);
			//System.out.println((int)msg[i]+" original");
			nums[i]=nums[i].pow(e.intValue());
			nums[i]=nums[i].mod(m);
			if(i>8) {//If solo para imprimir
				System.out.print(nums[i]+",");
			}
		}
		System.out.println();
		
		return nums;
	}
	
	public static String desencriptar(BigInteger priv, BigInteger m,BigInteger[] txt) {
		
		String tmp="";
		System.out.print("Desencriptado: ");
		for (int i = 0; i < txt.length; i++) {
			int total=txt[i].modPow(priv,m).intValue();
			tmp+=(char)total;
			if(i>8) {
				System.out.print(total+",");
			}
			//System.out.println(txt[i].pow(priv.intValue()).mod(m).intValue());
		}
		System.out.println();
		return tmp;
	}
	
	//Encontrar el inverso multiplicativo del módulo.
	
	public static BigInteger inversoM(BigInteger a, BigInteger m) 
    { 
        BigInteger m2 = m; 
        BigInteger y = BigInteger.ZERO, x = BigInteger.ONE; 
  
        if (m.compareTo(BigInteger.ONE) == 0) 
            return BigInteger.ZERO; 
  
        while (a.compareTo(BigInteger.ONE)>0) 
        { 
            // q es el cociente 
            BigInteger q = a.divide(m); 
  
            BigInteger t = m; 
  
            // m es el residuo
            m = a.mod(m); 
            a = t; 
            t = y; 
  
            // Actualizar valores
            y=x.subtract(q.multiply(y)); 
            x = t; 
        } 
  
        // X positivo
        
        if (x.compareTo(BigInteger.ZERO)<0) 
            x =x.add(m2); 
  
        return x; 
    }
	
	public static boolean esPrimo(int num){
        if (num <= 3 || num % 2 == 0) 
            return num == 2 || num == 3;
        int divisor = 3;
        while ((divisor <= Math.sqrt(num)) && (num % divisor != 0)) 
            divisor += 2;
        return num % divisor != 0;
    }
	
	//Método para obtener el máximo común divisor
	public static BigInteger mcd(BigInteger a, BigInteger b) 
    { 
        // Divide 0  
        if (a.compareTo(BigInteger.ZERO)==0 || b.compareTo(BigInteger.ZERO)==0) {
        	return BigInteger.ZERO;
        }
        // Caso de salida
        if (a.compareTo(b)==0) {
        	return a;
        }
        // A es mayor 
        if (a.compareTo(b)>0) {
        	return mcd(a.subtract(b), b); 
        }
                  
        return mcd(a, b.subtract(a)); 
    } 
	
	//Hacer y correr el servidor
	public void empezarServidor() {
		
		try {
			this.servidor=new ServerSocket(5000,10);
			while (true) {
				try {
					this.esperarConexion();//Empezar y esperar a que alguien se conecte
					this.prepararStreams();//Una vez conectado, crear conexion entre las pc's
					this.chateando();//Cuando estan conectados, poder enviar mensajes entre si
				} catch (EOFException e) {
					this.mostrarMensaje("\nTerminaste la conexión");
				}finally {
					this.cerrar();//Cerrar streams y sockets cuando se acabe el chat
				}
			}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void esperarConexion() throws IOException{
		this.mostrarMensaje("Esperando a alguien para hablar...");
		this.conexion=this.servidor.accept();
		this.mostrarMensaje("\nConexión con "+this.conexion.getInetAddress().getHostName()+" exitosa!");
	}
	
	private void prepararStreams() throws IOException{
		this.salida=new ObjectOutputStream(this.conexion.getOutputStream());//Lo que permite conectar a otra instancia
		this.salida.flush();//Liberar memoria
		this.entrada=new ObjectInputStream(this.conexion.getInputStream());//Lo mismo que salida pero conecta para recibir
		this.mostrarMensaje("\nLas conexiones están hechas!");
	}
	
	private void chateando() throws IOException{
		this.enviarLlavePub();//Envía su llave pública
		//Recibir llave pub
		try {
			this.kPubR=(BigInteger[]) this.entrada.readObject();
			System.out.println("Kpub recibida: "+kPubR[0]+", "+kPubR[1]);
		} catch (ClassNotFoundException e) {
			this.mostrarMensaje("\nError en la recepción de llaves");
		}
		String mensaje="Estás conectado";
		BigInteger[] paquete;
		this.enviarMensaje(mensaje);
		this.permitirEscritura(true);
		
		do {
			try {
				paquete=(BigInteger[]) this.entrada.readObject();
				mensaje=desencriptar(kPriv, kPub[0], paquete);
				this.mostrarMensaje("\n"+mensaje);
			} catch (ClassNotFoundException e) {
				this.mostrarMensaje("\nError en escritura");
			}
		} while (!mensaje.equals("Cliente ha salido"));
	}
	
	private void cerrar() {
		this.mostrarMensaje("\nCerrando conexiones...\n");
		this.permitirEscritura(false);
		try {
			
			this.salida.close();
			this.entrada.close();
			this.conexion.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void enviarMensaje(String mensaje) {
		try {
			BigInteger[] tmp=encriptar(this.kPubR[0], this.kPubR[1], "Servidor: "+mensaje);
			this.salida.writeObject(tmp);//Crea un objeto y lo manda a la otra instancia
			this.salida.flush();
			this.mostrarMensaje("\nServidor: "+mensaje);
		} catch (IOException e) {
			this.chatCompleto.append("\nError al enviar el mensaje");
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
	
	private void mostrarMensaje(final String mensaje) {//Actualiza la ventana de chatcompleto
		SwingUtilities.invokeLater(
				new Runnable() {
					
					@Override
					public void run() {
						chatCompleto.append(mensaje);
						
					}
				}
		);
	}
	
	private void permitirEscritura(final boolean bool) {
		SwingUtilities.invokeLater(
				new Runnable() {
					
					@Override
					public void run() {
						cerrar.setEnabled(bool);
						usuarioTxt.setEditable(bool);
						
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
		Servidor prueba=new Servidor();
		prueba.empezarServidor();
	}
}
