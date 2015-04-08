package com.nulldev.mgfetcher;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/*
 * Simple Java program to fetch Manga from MangaHere.co
 * 
 * Proudly hardcoded by: nulldev...
 */

public class MGFetcher {

	private JFrame frame;
	private JTextField txtUrl;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MGFetcher window = new MGFetcher();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MGFetcher() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel lblMgfetcher = new JLabel("MGFetcher by: nulldev");
		lblMgfetcher.setFont(new Font("Dialog", Font.BOLD | Font.ITALIC, 30));
		frame.getContentPane().add(lblMgfetcher, BorderLayout.NORTH);

		txtUrl = new JTextField();
		txtUrl.setText("URL");
		frame.getContentPane().add(txtUrl, BorderLayout.CENTER);
		txtUrl.setColumns(10);

		final JComboBox<String> comboBox = new JComboBox<String>();
		comboBox.addItem("MangaHere");
		frame.getContentPane().add(comboBox, BorderLayout.SOUTH);

		JButton btnGo = new JButton("GO");
		btnGo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String url = txtUrl.getText();
				String site = (String) comboBox.getSelectedItem();
				//FETCH MY FRIEND, FETCH
				try {
					Document mangaRoot = Jsoup.connect(url).get();
					String title = mangaRoot.title();
					System.out.println("Manga is: " + title);
					
					//PROUDLY HARDCODED FOR MANGAHERE
					//As you can see it is not very modular as everything is all in one "if" statement.
					//But it works :) and it was designed for personal use. Don't judge... Maybe I will move
					//some of the code into different functions later
					if(site.equals("MangaHere")) {
						title = title.split(" - ")[0];
						System.out.println("Parsing chapter list...");
						Element chapterList = mangaRoot.getElementsByClass("detail_list").get(0).getElementsByTag("ul").get(0);
						int totalChapters = chapterList.getElementsByTag("li").size();
						System.out.println("Found " + totalChapters + " chapters...");
						for(Element chapterListChild : chapterList.getElementsByTag("li")) {
							boolean metError = true;
							while(metError == true) {
								try {								
									Element leftEntry = chapterListChild.getElementsByClass("left").get(0);
									Element chapterLink = leftEntry.getElementsByTag("a").get(0);
									String chapterNumber = chapterLink.attr("href").split("/")[chapterLink.attr("href").split("/").length-1].replace("c", "");
									System.out.println("Found chapter: " + chapterLink.text() +", Chapter #: " + chapterNumber + ", downloading...");
									Document chapter = Jsoup.connect(chapterLink.attr("href")).get();
									Element pageList = chapter.getElementsByAttributeValue("onchange", "change_page(this)").get(0);
									boolean isTitlePage = true;
									File bookTarget = new File("downloads/" + title + "/c" + chapterNumber + "/BOOK.epub");
									int totalFiles = 0;
									if(new File("downloads/" + title + "/c" + chapterNumber + "/").exists()) {
										totalFiles = new File("downloads/" + title + "/c" + chapterNumber + "/").listFiles().length;
									}
									
									//Boox stuffz
									System.out.println("Creating new book...");
									Book book = new Book();
									book.getMetadata().setTitles(Arrays.asList(chapterLink.text()));
									book.getMetadata().addAuthor(new Author("nulldev - MGFetcher"));
									
									if(bookTarget.exists() && totalFiles-1 == pageList.getElementsByTag("option").size()) {
										System.out.println("Chapter already downloaded, skipping!");
									} else {
										for(Element pageListChild : pageList.getElementsByTag("option")) {
											System.out.println("    Found page: " + pageListChild.text() +", downloading...");
											File downloadTarget = new File("downloads/" + title + "/c" + chapterNumber + "/" + pageListChild.text() + ".jpg");
											new File("downloads/" + title + "/c" + chapterNumber + "/").mkdirs(); //Make the dirs...
											if(!downloadTarget.exists()) {
												Document page = Jsoup.connect(pageListChild.attr("value")).get();
												Element imageElement = page.getElementsByAttributeValue("onerror", "javascript:rerender(this);").get(0);
												FileUtils.copyURLToFile(new URL(imageElement.attr("src")), downloadTarget);
											} else {
												System.out.println("    Page already downloaded, skipping!");
											}
											System.out.println("    Done! (Downloaded to: " + downloadTarget.getAbsolutePath() + "!");
											if(!bookTarget.exists()) {
												if(isTitlePage) {
													System.out.println("    This is a title page! Setting book title page to current page!");
													//Set cover page
													book.setCoverImage(new Resource(new FileInputStream(downloadTarget), "cover.jpg"));
													String html = "<style>"
															+ "#bg {"
															+ "position: fixed; "
															+ "top: -50%; "
															+ "left: -50%; "
															+ "width: 200%; "
															+ "height: 200%;"
															+ "}"
															+ "#bg img {"
															+ "position: absolute; "
															+ "top: 0; "
															+ "left: 0; "
															+ "right: 0; "
															+ "bottom: 0; "
															+ "margin: auto; "
															+ "min-width: 50%;"
															+ "min-height: 50%;"
															+ "}"
															+ "</style><div id=\"bg\">"
															+ "<img src=\"cover.jpg\">"
															+ "</div>";
													InputStream fakeStream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
													book.addSection("Cover Page", new Resource(fakeStream, pageListChild.text() + "cover.html"));
													isTitlePage = false;
												} else {
													System.out.println("    Adding to EPUB...");
													String html = "<style>"
															+ "#bg {"
															+ "position: fixed; "
															+ "top: -50%; "
															+ "left: -50%; "
															+ "width: 200%; "
															+ "height: 200%;"
															+ "}"
															+ "#bg img {"
															+ "position: absolute; "
															+ "top: 0; "
															+ "left: 0; "
															+ "right: 0; "
															+ "bottom: 0; "
															+ "margin: auto; "
															+ "min-width: 50%;"
															+ "min-height: 50%;"
															+ "}"
															+ "</style><div id=\"bg\">"
															+ "<img src=\""+pageListChild.text() + "-epub.jpg\">"
															+ "</div>";
													InputStream fakeStream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
													book.addSection("Page: " + pageListChild.text(), new Resource(fakeStream, pageListChild.text() + "-epub.html"));
													book.addResource(new Resource(new FileInputStream(downloadTarget), pageListChild.text() + "-epub.jpg"));
												}
											}
										}
										if(!bookTarget.exists()) {
											System.out.println("    Writing book to disk (" + bookTarget.getAbsolutePath() + ")!");
											EpubWriter epubWriter = new EpubWriter();
											epubWriter.write(book, new FileOutputStream(bookTarget));
										} else {
											System.out.println("    Book already exists, skipping!");
										}
									}
									/*
									 * This was originally to prevent MangaHere from crashing the program if we did too much downloading...
									 * (The site would slow/prevent and more downloads after you have downloaded a specified amount of pages)
									 * (I have fixed this by just making it spam the server until it allows me to download again :P)
									 * 
									 * System.out.println("Waiting 10 seconds...");
								Thread.sleep(10000);*/
									metError = false;
								} catch(Exception e2) {
									System.out.println("MET ERROR, RETRYING!");
									e2.printStackTrace();
								}
							}
						}
						JOptionPane.showMessageDialog(null, "Done!");
					} else {
						System.out.println("Unknown site!");
					}
				} catch (Exception e1) {
					System.out.println("SHIT HAPPENED!");
					e1.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(btnGo, BorderLayout.EAST);
	}

}
