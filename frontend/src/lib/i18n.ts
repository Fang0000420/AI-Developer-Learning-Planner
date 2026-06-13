export type Locale = "zh" | "en";

export const LOCALE_COOKIE = "ai_planner_locale";

export function normalizeLocale(value: string | null | undefined): Locale {
  return value === "en" ? "en" : "zh";
}

export function responseLanguageLabel(language: Locale, locale: Locale) {
  if (locale === "zh") {
    return language === "zh" ? "中文" : "英文";
  }
  return language === "zh" ? "Chinese" : "English";
}

export const dictionaries = {
  zh: {
    metadata: {
      title: "AI 开发者学习规划器",
      description: "AI 学习规划器 MVP 前端工作台。",
    },
    nav: {
      dashboard: "工作台",
      goals: "目标",
      newGoal: "新目标",
      plans: "计划",
      knowledge: "知识库",
      today: "今日",
      agentRuns: "Agent 运行",
      login: "登录",
      workspace: "MVP 工作台",
      signOut: "退出登录",
    },
    common: {
      dashboard: "工作台",
      status: "状态",
      ready: "就绪",
      createGoal: "创建目标",
      newGoal: "新目标",
      openGoalForm: "打开目标表单",
      goal: "目标",
      goalId: "目标 #",
      plan: "计划",
      cycle: "周期",
      daily: "每日",
      updated: "更新",
      created: "创建",
      days: "天",
      hours: "小时",
      notSet: "未设置",
      reset: "重置",
      language: "语言",
      responseLanguage: "Agent 输出语言",
      chinese: "中文",
      english: "英文",
      noDescription: "未记录描述。",
      backendError: "后端接口没有响应。",
    },
    status: {
      ACTIVE: "进行中",
      COMPLETED: "已完成",
      PAUSED: "已暂停",
      CANCELLED: "已取消",
      PENDING: "待开始",
      IN_PROGRESS: "进行中",
      DONE: "已完成",
      SKIPPED: "已跳过",
      none: "无影响",
      minor: "轻微影响",
      medium: "中等影响",
      major: "严重影响",
    },
    home: {
      badge: "Day 15 演示链路",
      title: "构建自适应 AI 学习计划",
      description:
        "在这个工作台中创建学习目标、查看生成计划、提交每日进度，并验证下一天任务是否按复盘结果调整。",
      statusCards: [
        {
          label: "当前目标",
          value: "已保存流程",
          detail: "目标会持久化到 Spring Boot，并驱动 Agent 链路。",
        },
        {
          label: "计划窗口",
          value: "7 到 60 天",
          detail: "计划生成会保存学习计划和每日任务。",
        },
        {
          label: "今日任务",
          value: "可自适应",
          detail: "进度复盘可以调整下一天的任务列表。",
        },
      ],
      quickTitle: "工作台入口",
      quickSubtitle: "MVP 流程",
      quickActions: [
        {
          title: "创建目标",
          description: "记录背景、目标岗位、时间预算和计划周期。",
          href: "/goals/new",
        },
        {
          title: "目标库",
          description: "浏览已保存目标，并打开后端驱动的目标详情。",
          href: "/goals",
        },
        {
          title: "查看计划",
          description: "查看生成的学习路线和项目方向。",
          href: "/plans",
        },
        {
          title: "今日任务",
          description: "推进每日任务，并准备进度记录。",
          href: "/tasks/today",
        },
      ],
      workflow: [
        "能力画像",
        "目标拆解",
        "技能差距分析",
        "项目推荐",
        "每日计划",
        "进度复盘",
        "计划调整",
      ],
      progressTitle: "MVP 进度",
      nextTitle: "下一步",
      nextDescription:
        "创建一个目标，在目标详情页运行演示链路，打开第 1 天任务，并提交带有一个未完成任务的进度。",
    },
    goals: {
      badge: "目标管理",
      title: "学习目标",
      description: "查看后端保存的学习目标，并打开目标继续 MVP 规划流程。",
      unavailableTitle: "目标不可用",
      emptyTitle: "还没有目标",
      emptyDescription:
        "创建第一个学习目标，将它保存到后端，并用于后续能力画像和计划生成。",
      operationFailed: "目标操作失败。",
      createFailed: "目标创建失败。",
    },
    newGoal: {
      badge: "目标输入",
      title: "新的学习目标",
      description: "记录 MVP 能力画像和计划生成所需的第一批输入。",
      saveStatus: "保存到后端",
      technicalBackground: "技术背景",
      technicalPlaceholder: "例如：有 Java 和 PostgreSQL 经验的后端开发者...",
      learningGoal: "学习目标",
      learningPlaceholder: "例如：构建 AI Agent 应用并理解生产化流程...",
      jobTarget: "目标方向",
      jobPlaceholder: "AI 工程师",
      dailyHours: "每日小时数",
      planCycle: "计划周期",
      createStatus: "创建状态",
      summary: "目标摘要",
      creating: "创建中...",
      submit: "创建目标",
      unable: "无法创建目标。",
      validHint: "有效提交会保存到后端，并从目标详情页打开。",
      validation: {
        technicalBackground: "请至少用 10 个字符描述你的技术背景。",
        technicalBackgroundMax: "技术背景不能超过 3000 个字符。",
        learningGoal: "请至少用 10 个字符描述你的学习目标。",
        learningGoalMax: "学习目标不能超过 255 个字符。",
        jobTarget: "请输入目标岗位或职业方向。",
        jobTargetMax: "目标方向不能超过 120 个字符。",
        dailyMin: "每日时间至少为 0.5 小时。",
        dailyMax: "每日时间不能超过 12 小时。",
        planCycleMin: "计划周期至少为 7 天。",
        planCycleMax: "计划周期不能超过 60 天。",
        planCycleInt: "计划周期必须为整数天数。",
      },
    },
    auth: {
      signIn: "登录",
      register: "注册",
      createAccount: "创建账号",
      loginDescription: "继续使用你的学习规划工作台。",
      registerDescription: "创建账号来管理自己的目标和计划。",
      username: "用户名",
      email: "邮箱",
      password: "密码",
      working: "处理中...",
      failed: "认证失败。",
    },
  },
  en: {
    metadata: {
      title: "AI Developer Learning Planner",
      description: "Frontend workspace for the AI learning planner MVP.",
    },
    nav: {
      dashboard: "Dashboard",
      goals: "Goals",
      newGoal: "New Goal",
      plans: "Plans",
      knowledge: "Knowledge",
      today: "Today",
      agentRuns: "Agent Runs",
      login: "Login",
      workspace: "MVP workspace",
      signOut: "Sign out",
    },
    common: {
      dashboard: "Dashboard",
      status: "Status",
      ready: "Ready",
      createGoal: "Create Goal",
      newGoal: "New Goal",
      openGoalForm: "Open Goal Form",
      goal: "Goal",
      goalId: "Goal #",
      plan: "Plan",
      cycle: "Cycle",
      daily: "Daily",
      updated: "Updated",
      created: "Created",
      days: "days",
      hours: "hours",
      notSet: "Not set",
      reset: "Reset",
      language: "Language",
      responseLanguage: "Agent output language",
      chinese: "Chinese",
      english: "English",
      noDescription: "No description recorded.",
      backendError: "The backend API did not respond.",
    },
    status: {
      ACTIVE: "Active",
      COMPLETED: "Completed",
      PAUSED: "Paused",
      CANCELLED: "Cancelled",
      PENDING: "Pending",
      IN_PROGRESS: "In progress",
      DONE: "Done",
      SKIPPED: "Skipped",
      none: "No impact",
      minor: "Minor impact",
      medium: "Medium impact",
      major: "Major impact",
    },
    home: {
      badge: "Day 15 demo flow",
      title: "Build an adaptive AI learning plan",
      description:
        "Use this workspace to create a learning goal, inspect the generated plan, submit daily progress, and verify the next day adjusted task list.",
      statusCards: [
        {
          label: "Active goal",
          value: "Saved flow",
          detail: "Goals persist through Spring Boot and feed the agent chain.",
        },
        {
          label: "Plan window",
          value: "7 to 60 days",
          detail: "Plan generation stores learning plans and daily tasks.",
        },
        {
          label: "Today",
          value: "Adaptive",
          detail: "Progress review can adjust the next day's task list.",
        },
      ],
      quickTitle: "Workspace Entry Points",
      quickSubtitle: "MVP flow",
      quickActions: [
        {
          title: "Create Goal",
          description:
            "Capture background, target role, time budget, and plan cycle.",
          href: "/goals/new",
        },
        {
          title: "Goal Library",
          description:
            "Browse saved goals and open backend-backed goal details.",
          href: "/goals",
        },
        {
          title: "View Plan",
          description:
            "Review the generated learning roadmap and project direction.",
          href: "/plans",
        },
        {
          title: "Today Tasks",
          description: "Work through daily tasks and prepare progress notes.",
          href: "/tasks/today",
        },
      ],
      workflow: [
        "Profile analysis",
        "Goal decomposition",
        "Skill gap review",
        "Project recommendation",
        "Daily plan",
        "Progress review",
        "Plan adjustment",
      ],
      progressTitle: "MVP Progress",
      nextTitle: "Next Step",
      nextDescription:
        "Create a goal, run the demo chain from the goal detail page, open Day 1 tasks, and submit progress with one unfinished task.",
    },
    goals: {
      badge: "Goal management",
      title: "Learning Goals",
      description:
        "Review saved learning goals from the backend and open a goal to continue the MVP planning flow.",
      unavailableTitle: "Goals unavailable",
      emptyTitle: "No goals yet",
      emptyDescription:
        "Create the first learning goal to persist it in the backend and use it for upcoming profile analysis and planning.",
      operationFailed: "Goal operation failed.",
      createFailed: "Goal creation failed.",
    },
    newGoal: {
      badge: "Goal intake",
      title: "New Learning Goal",
      description:
        "Capture the first planning inputs for the MVP profile analysis and plan generation flow.",
      saveStatus: "Saves to backend",
      technicalBackground: "Technical Background",
      technicalPlaceholder:
        "Backend developer with Java and PostgreSQL experience...",
      learningGoal: "Learning Goal",
      learningPlaceholder:
        "Build AI agent applications and understand production workflows...",
      jobTarget: "Job Target",
      jobPlaceholder: "AI Engineer",
      dailyHours: "Daily Hours",
      planCycle: "Plan Cycle",
      createStatus: "Create Status",
      summary: "Goal Summary",
      creating: "Creating...",
      submit: "Create Goal",
      unable: "Unable to create goal.",
      validHint:
        "Valid submissions are saved to the backend and opened from the goal detail page.",
      validation: {
        technicalBackground:
          "Describe your background in at least 10 characters.",
        technicalBackgroundMax:
          "Technical background cannot exceed 3000 characters.",
        learningGoal: "Describe your learning goal in at least 10 characters.",
        learningGoalMax: "Learning goal cannot exceed 255 characters.",
        jobTarget: "Enter a target role or career direction.",
        jobTargetMax: "Job target cannot exceed 120 characters.",
        dailyMin: "Daily time must be at least 0.5 hours.",
        dailyMax: "Daily time cannot exceed 12 hours.",
        planCycleMin: "Plan cycle must be at least 7 days.",
        planCycleMax: "Plan cycle cannot exceed 60 days.",
        planCycleInt: "Plan cycle must be a whole number of days.",
      },
    },
    auth: {
      signIn: "Sign in",
      register: "Register",
      createAccount: "Create account",
      loginDescription: "Continue your learning planner workspace.",
      registerDescription:
        "Create a planner account for your own goals and plans.",
      username: "Username",
      email: "Email",
      password: "Password",
      working: "Working...",
      failed: "Authentication failed.",
    },
  },
} as const;

export type Dictionary = (typeof dictionaries)[Locale];
