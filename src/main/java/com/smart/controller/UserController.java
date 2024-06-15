package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private UserRepository userRepository;
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
	
		String userName = principal.getName();
		System.out.println("USERNAME"+userName);
		User user = userRepository.getUserByUserName(userName);
System.out.println("USER"+user);
		model.addAttribute("user",user);
	}
	
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {
		
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard"; 
		
	}
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact , @RequestParam("profileImage") MultipartFile file, Principal principal,HttpSession session){
	
	try {
	
	String name = principal.getName();
	User user = this.userRepository.getUserByUserName(name);
	
	
	
	if(file.isEmpty()) {
		System.out.println("file is empty");
		contact.setImage("contact.png");
	}
	else {
		contact.setImage(file.getOriginalFilename());
		File saveFile=new ClassPathResource("static/img").getFile();
		
		Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
		Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
		System.out.println("image is uploaded");
	}
	
	contact.setUser(user);
	user.getContacts().add(contact);
	
	this.userRepository.save(user);

	System.out.println("DATA"+contact);
	System.out.println("added to data base");
	
	session.setAttribute("message", new Message("your contact is added","success"));
	
	}catch(Exception e) {
		System.out.println("ERROR"+e.getMessage());
		e.printStackTrace();
		session.setAttribute("message", new Message("Something went wrong","danger"));
		
	}
		return "normal/add_contact_form";
	
	}
@GetMapping("/show-contacts")
public String showContacts(Model m,Principal principal) {
	m.addAttribute("title","show user contacts");
	
	String userName = principal.getName();
	User user = this.userRepository.getUserByUserName(userName);
	
	List<Contact> contacts = this.contactRepository.findContactsByUser(user.getId());
	
	m.addAttribute("contacts" ,contacts);
	
	return "normal/show_Contacts";
	
}
@RequestMapping("/{cId}/contact")
public String showContactDetail(@PathVariable("cId") Integer cId ,Model model) {
	System.out.println("CID"+cId);
	
	Optional<Contact> contactOptional = this.contactRepository.findById(cId);
	
	Contact contact = contactOptional.get();
	
	model.addAttribute("contact",contact);
	
	
	return "normal/contact_detail";
	
}
@GetMapping("/delete/{cid}")
@Transactional
public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,
		Principal principal) {
	System.out.println("CID " + cId);

	Contact contact = this.contactRepository.findById(cId).get();
	

	User user = this.userRepository.getUserByUserName(principal.getName());

	user.getContacts().remove(contact);

	this.userRepository.save(user);

	System.out.println("DELETED");
	session.setAttribute("message", new Message("Contact deleted succesfully...", "success"));

	return "redirect:/user/show-contacts";
}
@GetMapping("/profile")
public String yourProfile(Model model) {
	model.addAttribute("title","Profile page");
	
	return "normal/profile";
}

@PostMapping("/update-contact/{cid}")
public String updateForm( @PathVariable("cid") Integer cid ,Model m) {
	m.addAttribute("titile"," Update Contact" );
	
	Contact  contact = this.contactRepository.findById(cid).get();
	
	m.addAttribute("contact", contact);
	
	return "normal/update_form";
}

@RequestMapping(value = "/process-update", method = RequestMethod.POST)
public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
		Model m, HttpSession session, Principal principal) {

	try {

		// old contact details
		Contact oldcontactDetail = this.contactRepository.findById(contact.getCid()).get();

		// image..
		if (!file.isEmpty()) {
			
			File deleteFile = new ClassPathResource("static/img").getFile();
			File file1 = new File(deleteFile, oldcontactDetail.getImage());
			file1.delete();



			File saveFile = new ClassPathResource("static/img").getFile();

			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			contact.setImage(file.getOriginalFilename());

		} else {
			contact.setImage(oldcontactDetail.getImage());
		}

		User user = this.userRepository.getUserByUserName(principal.getName());

		contact.setUser(user);

		this.contactRepository.save(contact);

		session.setAttribute("message", new Message("Your contact is updated...", "success"));

	} catch (Exception e) {
		e.printStackTrace();
	}

	System.out.println("CONTACT NAME " + contact.getName());
	System.out.println("CONTACT ID " + contact.getCid());
	return "redirect:/user/" + contact.getCid() + "/contact";
}



}
