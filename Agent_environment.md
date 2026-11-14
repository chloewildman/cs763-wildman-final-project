# Autonomous AI Agent
Agent: GitHub Copilot
Model: GPT-6 Luna - Effort level: High - Context size: 272k 
- Other hyperparameters aren't supported (https://developers.openai.com/api/docs/models/gpt-6-luna; https://reapi.ai/blog/gpt-6-luna-temperature)
Mode: Autopilot (works autonomously within permissions)
Permissions: Manual permissions (Asks when approval settings don't apply)
Manual permissions:
- Sandbox: Off
- Allow outbound connections
- Local network access disabled
- Allow GitHub integration
- Allow GitHub CLI integration
- Allow dev tool access
- Enable risk assessment (LLM-generated risk level - Safe/Caution/Review carefully)

# Host Environment
- Operating System: Windows 11
- Java: OpenJDK/Oracle JDK 25.0.1
- IDE: VS Code v.1.144.0

# Application Stack
- Language: Java
- Framework: Spring Boot
- Source Control: Git/GitHub
- CI/CD: GitHub Actions
- Containerization: Docker

# Tools
- Internet searching
- Git integration
- File editing
- Search of the codebase
- Terminal commands (execution might require approval, see above)