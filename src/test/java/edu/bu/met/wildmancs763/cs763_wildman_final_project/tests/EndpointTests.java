package edu.bu.met.wildmancs763.cs763_wildman_final_project.tests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsProjectCollectionsUsingTheSpecContract() throws Exception {
		mockMvc.perform(get("/courses/1/projects"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].projectId").value(102))
				.andExpect(jsonPath("$[0].projectTitle").value("Research planning"))
				.andExpect(jsonPath("$[0].courseId").value(1))
				.andExpect(jsonPath("$[0].projectCreationTimestamp").value("2026-03-01T10:00:00Z"))
				.andExpect(jsonPath("$[1].projectId").value(101))
				.andExpect(jsonPath("$[1].projectCreationTimestamp").value("2026-02-01T10:00:00Z"));

		mockMvc.perform(get("/courses/2/projects"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/courses/999/projects"))
				.andExpect(status().isNotFound());
	}
}
