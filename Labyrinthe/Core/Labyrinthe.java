package Core;

import java.io.File;
import java.util.ArrayList;
//import java.util.List;
import java.util.Scanner;
//import java.util.Stack;


import org.graphstream.algorithm.Dijkstra;
import org.graphstream.algorithm.Kruskal;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
//import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.ui.swingViewer.Viewer;

/**
 * TP Graphes : Construction d'un labyrinthe et création d'un chemin de l'entrèe vers la sortie.
 * @author : CHIBANE MOURAD.
 * Université du Havre, Master 1 Mathématique et informatique 
 */
public class Labyrinthe {
	int taille, nbCells;
	String rDebut, rFin;
	Graph grille;
	
	/**
	 * @param : Taille de la grille. 
	 */
	public Labyrinthe(int taille){
		this.taille=taille;
		this.nbCells=taille*taille;
		grille=generateGrille(taille);
		grille.setAttribute("ui.quality");
		grille.setAttribute("ui.antialias");
	}
	
	/**
	 * Construit un graphe dual Composé d'une grille de murs et d'une grille de cellules.
	 * @param taille_grille Hauteur du labyrinthe.
	 */
	public Graph generateGrille(int taille_grille){
		taille=taille_grille;
		Graph g=new DefaultGraph("Labyrinthe", true, true);
		int nodeNameVal=0, resolutionPathId=0;
		
		for(int i=0;i<taille;i++){
			for(int j=0; j<taille; j++){
				String nodeName = Integer.toString(nodeNameVal);
				
				g.addNode(nodeName);
				g.getNode(nodeName).addAttribute("xy", i, j);
				g.getNode(nodeName).addAttribute("ui.style", "size: 1px;");
				
				/**
				 * On doit poser les noeuds à l'intérieur de la grilles de murs 
				 * donc On ne pose plus dans le second graphe lorsque l'on atteint le mur
				 * de droite ou du haut.
				 */
				if(nodeNameVal<nbCells-taille && (nodeNameVal+1)%taille!=0){
					g.addNode("r-"+resolutionPathId);
					g.getNode("r-"+resolutionPathId).addAttribute("xy", i+0.5, j+0.5);
					g.getNode("r-"+resolutionPathId).addAttribute("ui.style", "size: 1px;");
				}
				
				/*
				 * Définition des poids aux arêtes une fois le premier noeud posé et 
				 * faire les liaisons.
				 */
				if(nodeNameVal>0){
					String previousVal=Integer.toString(nodeNameVal-1);
					String previousLineNode=Integer.toString(nodeNameVal-taille);
					
					if(nodeNameVal%taille!=0){
						g.addEdge(previousVal+nodeName, previousVal, nodeName);
						
						if((nodeNameVal<taille && nodeNameVal-1<taille) || 
						   (nodeNameVal>nbCells-taille-1 && nodeNameVal-1>nbCells-taille-1)){
							/*
							 * Définition d'un poids nul quand il s'agit d'un mur extérieur à droite ou a gauche
							 * Tout en faisant attention à ne pas casser les murs extérieur.
							 */
							g.getEdge(previousVal+nodeName).addAttribute("weight", 0.0);
						}
						else
							/* Définition d'un poids au hasard plus grand que celui des murs extérieurs. */
							g.getEdge(previousVal+nodeName).addAttribute("weight", Math.random()*100+1);
					}
					if(nodeNameVal>taille-1){
						g.addEdge(previousLineNode+nodeName, previousLineNode, nodeName);
						
						if(nodeNameVal>taille-1){
							if(((nodeNameVal%taille)==0 && (nodeNameVal-taille)%taille==0) ||
							   (((nodeNameVal+1)%taille)==0 && ((nodeNameVal-taille)+1)%taille==0)){
								/* Définition d'un poids nul quand il s'agit d'un mur en haut ou en bas */
								g.getEdge(previousLineNode+nodeName).addAttribute("weight", 0.0);
							}
							else
					   		/* Définition d'un poids au hasard plus grand que celui des murs extérieurs. */
								g.getEdge(previousLineNode+nodeName).addAttribute("weight", Math.random()*100+1);
						}
					}
				}
					
				/*arêtes verticales pour la grille de résolution*/
				if(resolutionPathId>0 && resolutionPathId%(taille-1)!=0){
					g.addEdge("r-"+(resolutionPathId-1)+"-"+resolutionPathId, "r-"+(resolutionPathId-1), "r-"+resolutionPathId);
					g.getEdge("r-"+(resolutionPathId-1)+"-"+resolutionPathId).addAttribute("ui.style", "fill-color: white;");
					
					/*Poids aléatoire pour la génération du chemin */
					g.getEdge("r-"+(resolutionPathId-1)+"-"+resolutionPathId).addAttribute("weight", Math.random());
				}
				
				/*arête horizontales pour la grille de résolution, 
				 * 1ere condition : ne pas poser hors de la grille.
				 */
				if(g.getNode("r-"+resolutionPathId)!=null && resolutionPathId>=taille-1 && resolutionPathId<nbCells-(taille*2)+1){
				
					g.addEdge("r-"+(resolutionPathId-(taille-1))+"-"+resolutionPathId, "r-"+(resolutionPathId-(taille-1)), "r-"+resolutionPathId);
					g.getEdge("r-"+(resolutionPathId-(taille-1))+"-"+resolutionPathId).addAttribute("ui.style", "fill-color: white;");
					
					/*Poids aléatoire pour la génération du chemin */
					g.getEdge("r-"+(resolutionPathId-(taille-1))+"-"+resolutionPathId).addAttribute("weight", Math.random());
				}

				if(nodeNameVal<nbCells-taille && (nodeNameVal+1)%taille!=0)
					resolutionPathId++;

				nodeNameVal++;
			}
		}
		
		return g;
	}
	
	/**
	 * Pour éviter les secteurs isolés on Calcule l'arbre couvrant du graphe.
	 * Les bord sont inclus car les arêtes sur les bord ont un poids de 0.
	 */
	public void spanningTree(){
		ArrayList<String> oldEdges=new ArrayList<String>();
		Kruskal p=new Kruskal("weight", "inTree");
		p.init(grille);
		p.compute();
		
		for(Edge e:grille.getEachEdge()){
			if(!e.getId().startsWith("r-")){
				if(!((Boolean)e.getAttribute("inTree")))
					oldEdges.add(e.getId());
			}
		}
		
		for(String e:oldEdges)
			grille.removeEdge(e);
	}
	
	/**
	 * Définition aléatoire de l'entrée et la sortie puis génèrer la destruction des murs
	 */
	public void desructWall(){
		int entree=(int)(Math.random()*taille);
		int sortie=(int)(nbCells-taille+Math.random()*taille);
		
		
		int startSide, exitSide;

		if(entree==0)
			startSide=1;
		else if(entree==taille-1)
			startSide=entree-1;
		else
			startSide=(Math.random()>0.5)?entree+1:entree-1;

		grille.removeEdge(Integer.toString(startSide), Integer.toString(entree));

		if(sortie==nbCells-taille)
			exitSide=sortie+1;
		else if(sortie==nbCells-1)
			exitSide=sortie-1;
		else
			exitSide=(Math.random()>0.5)?sortie+1:sortie-1;

		grille.removeEdge(Integer.toString(exitSide), Integer.toString(sortie));

		int startIdx=(entree<startSide)?entree:startSide;
		int endIdx=((sortie<exitSide)?sortie:exitSide)-(2*(taille-1));
		
		rDebut="r-"+startIdx;
		rFin="r-"+endIdx;
		
		/* calcule le plus court chemin de l'entrée vers la sortie 
		 * et creuse le labyrinthe */
		this.constructPath();
	}	
	/**
	 * Calculer la valeur absolue de x
	 */
	public int abs(int x){
		return x<0?-x:x;
	}
	/**
	 * Afficher notre labyrinthe 
	 */
	public Viewer display(){
		return grille.display(false);
	}
	
	/**
	 * Calcul le plus court chemin depuis l'entrée à la sortie, creuse les murs autours du chemin.
	 */
	public void constructPath(){
		Dijkstra d=new Dijkstra(Dijkstra.Element.valueOf("edge"), "weight", rDebut);
		
		d.init(grille);
		d.compute();
		
		Path p=d.getShortestPath(grille.getNode(rFin));
		
		int source, dest;
		
		for(Edge e:p.getEdgePath()){
			e.addAttribute("ui.style", "fill-color: green;");
			e.setAttribute("ui.style", "size: 4.5px;");
			source=Integer.parseInt(e.getNode0().getId().split("r-")[1]);
			dest=Integer.parseInt(e.getNode1().getId().split("r-")[1]);
			
			double mod;
			
			if(abs(source-dest)>1){
				if(source<taille-2){
					mod=1;
				}
				else
					mod=dest/(taille-1);
				
				int bottom=(int)dest+Math.round((float)mod);
				int up=bottom+1;
				
				grille.removeEdge(Integer.toString(bottom)+Integer.toString(up));
			}
			else{
				int bottom=dest+(dest/(taille-1));
				int up=bottom+taille;
				
				grille.removeEdge(Integer.toString(bottom)+Integer.toString(up));
			}
			
		}
	}
	
	/**
	 * Récupère les arguments passés aux programmes
	 * @param arg l'argument à trouver
	 * @param set l'ensemble des arguments passés
	 * @return l'index de l'argument si trouvé, -1 sinon
	 */
	public static int indexOf(String arg, String[] set){
		for(int i=0;i<set.length;i++){
			if(set[i].equals(arg))
				return i;
		}
		
		return -1;
	}
	
	/**
	 * Enregistre le labyrinthe
	 * @param path Chemin d'enregistrement
	 */
	public void save(String path){
		try{
			File file=new File(path);
			
			if(file.exists()){
				Scanner s=new Scanner(System.in);
				String erase="";
				
				while(!(erase.toLowerCase().equals("oui") || erase.toLowerCase().equals("non"))){
					System.out.println("Le fichier "+path+" existe, voulez vous écraser le fichier ? [oui/non]");
					erase=s.nextLine().trim();
				}
				
				if(erase.toLowerCase().equals("oui")){
					grille.write(path);
					System.out.println("Fichier sauvgardé.");
					System.exit(1);
				}
				else{
					System.out.println("Fermeture du programme.");
					System.exit(1);
				}
			}
			else
				grille.write(path);
		}catch(Exception e){
			System.out.println("Erreur lors de l'enregistrement: "+e.getMessage());
		}
	}
	
	/**
	 * Affiche le manuel:
	 * -t pour spécifier la taille du labyrinthe.
	 * -S "Save" pour la sauvegarde.
	 * -L "Load" charge un labyrinthe contenu dans  un fichier *.dgs. 
	 */
	public static void help(){
		System.out.println("Trop peu d'arguments");
		
		System.out.println("Usage: java -jar Labyrinthe.jar args\n");
		System.out.println("Options: \n" +
						   "-t taille: Pour donner la taille du labyrinthe\n"+
						   "-Save fichier.dgs : Pour enregistrer le labyrinthe dans un fichier d'enregistrement\n"+
						   "-Load fichier.dgs : Pour charger un labyrinthe existant, contenu dans le fichier à spécifier");
		
		System.exit(1);
	}
	
	public static void main(String[] args) {
		if(args.length<2){
			help();
		}
		else{
			int sizeArg=indexOf("-t", args);
			
			if(sizeArg>-1 && sizeArg<args.length-1){
				int taille=Integer.parseInt(args[sizeArg+1]);
				
				Labyrinthe l=new Labyrinthe(taille);
				
				l.display();
				l.desructWall();
				l.spanningTree();
				
				int saveArg=indexOf("-Save", args);
				
				if(saveArg>-1 && saveArg<args.length-1){
					String saveFile=args[saveArg+1];
					
					l.save(saveFile);
				}
			}
			else{
				int loadArg=indexOf("-Load", args);
				
				if(loadArg>-1 && loadArg<args.length-1){
					String loadFile=args[loadArg+1];
					
					Graph g=new DefaultGraph("Labyrinthe", false, false);
					
					try{
						g.read(loadFile);
						g.display(false);
					}catch(Exception e){
						System.out.println("Erreur dans la lecture du fichier: "+e.getMessage());
					}
				}
				else
					help();
			}
		}
	}
}

