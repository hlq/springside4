package org.springside.examples.quickstart.web.task;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springside.examples.quickstart.entity.Thread;
import org.springside.examples.quickstart.entity.Task;
import org.springside.examples.quickstart.entity.User;
import org.springside.examples.quickstart.service.account.ShiroDbRealm.ShiroUser;
import org.springside.examples.quickstart.service.task.TaskService;
import org.springside.examples.quickstart.service.thread.ThreadService;
import org.springside.modules.web.Servlets;

import com.google.common.collect.Maps;

/**
 * Task管理的Controller, 使用Restful风格的Urls:
 * 
 * List page : GET /task/ Create page : GET /task/create Create action : POST
 * /task/create Update page : GET /task/update/{id} Update action : POST
 * /task/update Delete action : GET /task/delete/{id}
 * 
 * @author calvin
 */
@Controller
@RequestMapping(value = "/task")
public class TaskController {

	private static final String PAGE_SIZE = "3";

	private static Map<String, String> sortTypes = Maps.newLinkedHashMap();
	static {
		sortTypes.put("auto", "自动");
		sortTypes.put("title", "标题");
	}

	@Autowired
	private TaskService taskService;

	@Autowired
	private ThreadService threadService;

	@RequestMapping(value = "")
	public String list(
			@RequestParam(value = "page", defaultValue = "1") int pageNumber,
			@RequestParam(value = "page.size", defaultValue = PAGE_SIZE) int pageSize,
			@RequestParam(value = "sortType", defaultValue = "auto") String sortType,
			Model model, ServletRequest request) {
		Map<String, Object> searchParams = Servlets.getParametersStartingWith(
				request, "search_");
		Long userId = getCurrentUserId();

		Page<Task> tasks = taskService.getUserTask(userId, searchParams,
				pageNumber, pageSize, sortType);

		model.addAttribute("tasks", tasks);
		model.addAttribute("sortType", sortType);
		model.addAttribute("sortTypes", sortTypes);
		// 将搜索条件编码成字符串，用于排序，分页的URL
		model.addAttribute("searchParams", Servlets
				.encodeParameterStringWithPrefix(searchParams, "search_"));

		return "task/taskList";
	}

	// Checked
	@RequestMapping(value = "create/{threadId}", method = RequestMethod.GET)
	public String createForm(@PathVariable("threadId") Long threadId,
			Model model) {
		Thread thread = threadService.getThread(threadId);
		Task task = new Task();
		task.setThread(thread);
		model.addAttribute("task", task);
		model.addAttribute("action", "create");
		return "task/taskForm";
	}

	// Checked
	@RequestMapping(value = "create", method = RequestMethod.POST)
	public String create(
			@Valid Task newTask,
			@RequestParam(value = "thread_id", defaultValue = "0") long threadId,
			HttpServletRequest request, RedirectAttributes redirectAttributes) {
		// System.out.println(request.getParameter("thread_id"));
		User user = new User(getCurrentUserId());
		Thread thread = threadService.getThread(threadId);
		newTask.setUser(user);
		newTask.setThread(thread);

		taskService.saveTask(newTask);
		redirectAttributes.addFlashAttribute("message", "Success To Add Task");
		return "redirect:/thread/";
	}

	// Checked
	@RequestMapping(value = "update/{id}", method = RequestMethod.GET)
	public String updateForm(@PathVariable("id") Long id, Model model) {
		model.addAttribute("task", taskService.getTask(id));
		model.addAttribute("action", "update");
		return "task/taskForm";
	}

	// Checked
	@RequestMapping(value = "update", method = RequestMethod.POST)
	public String update(@Valid @ModelAttribute("task") Task task,
			RedirectAttributes redirectAttributes) {
		taskService.saveTask(task);
		redirectAttributes.addFlashAttribute("message", "更新任务成功");
		return "redirect:/thread/update/" + task.getThread().getId();
	}

	// Checked
	@RequestMapping(value = "delete/{id}")
	public String delete(@PathVariable("id") Long id,
			RedirectAttributes redirectAttributes) {
		taskService.deleteTask(id);
		redirectAttributes.addFlashAttribute("message", "删除任务成功");
		return "redirect:/thread/";
	}

	/**
	 * 所有RequestMapping方法调用前的Model准备方法, 实现Struts2
	 * Preparable二次部分绑定的效果,先根据form的id从数据库查出Task对象,再把Form提交的内容绑定到该对象上。
	 * 因为仅update()方法的form中有id属性，因此仅在update时实际执行.
	 */
	@ModelAttribute()
	public void getTask(@RequestParam(value = "id", required = false) Long id,
			Model model) {
		if (id != null) {
			model.addAttribute("task", taskService.getTask(id));
		}
	}

	/**
	 * 取出Shiro中的当前用户Id.
	 */
	private Long getCurrentUserId() {
		ShiroUser user = (ShiroUser) SecurityUtils.getSubject().getPrincipal();
		return user.id;
	}
}
