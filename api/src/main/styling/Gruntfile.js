/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var fs = require('fs');

module.exports = function (grunt) {

    // load all grunt tasks automatically
    require('load-grunt-tasks')(grunt);
    // display execution time of each task
    require('time-grunt')(grunt);
    
    var buildConfig = require('./build.config.js');
    function classPathExists() {
        return fs.existsSync(buildConfig.cp + '/skin/hippo-cms');
    }

    grunt.initConfig({

        build: buildConfig,
        
        // Watch for file changes and run corresponding tasks
        watch: {
            options: {
                livereload: true,
                interrupt: false,
                livereloadOnError: false
            },
            gruntfile: {
                files: ['Gruntfile.js']
            },
            less: {
                options: {
                    livereload: false
                },
                files: ['<%= build.src %>/**/*.less'],
                tasks: ['less', 'autoprefixer', /*'csslint',*/ 'concat', 'clean:target']
            },
            livereload: {
                files: ['<%= build.dest %>/**'],
                tasks: ['copy:classpath', 'shell:notify']
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '<%= build.tmp %>/css/<%= build.file %>.css': '<%= build.src %>/less/main.less'
                }
            },
            vendors: {
                files: {
                    '<%= build.tmp %>/css/open-sans.css':    '<%= build.src %>/less/lib/open-sans.less',
                    '<%= build.tmp %>/css/font-awesome.css': '<%= build.src %>/less/lib/font-awesome.less',
                    '<%= build.tmp %>/css/wicket.css':       '<%= build.src %>/less/lib/wicket.less',
                    '<%= build.tmp %>/css/style-test.css':   '<%= build.src %>/less/lib/style-test.less'
                }
            }
        },

        // Autoprefix vendor prefixes
        autoprefixer: {
            theme: {
                options: {
                    browsers: ['> 0%']
                },
                src: '<%= build.tmp %>/css/<%= build.file %>.css',
                dest: '<%= build.tmp %>/css/<%= build.file %>.css'
            }
        },

        // Lint the css output
        csslint: {
            lessOutput: {
                options: {
                    csslintrc: '.csslintrc'
                },
                src: ['<%= build.tmp %>/css/<%= build.file %>.css']
            }
        },

        // Minify CSS files
        cssmin: {
            options: {
                report: 'min'
            },
            theme: {
                files: {
                    '<%= build.dest %>/css/<%= build.file %>.min.css': ['<%= build.dest %>/css/<%= build.file %>.css']
                }
            }
        },

        // Concat files
        concat: {
            options: {
                stripBanners: true
            },
            css: {
                src: [
                    '<%= build.tmp %>/css/open-sans.css', 
                    '<%= build.tmp %>/css/font-awesome.css', 
                    '<%= build.bower %>/normalize.css/normalize.css', 
                    '<%= build.tmp %>/css/wicket.css', 
                    '<%= build.tmp %>/css/style-test.css', 
                    '<%= build.tmp %>/css/<%= build.file %>.css'
                ],
                dest: '<%= build.dest %>/css/<%= build.file %>.css'
            }
        },

        copy: {
            binaries: {
                files: [
                    {
                        expand: true,
                        cwd: '<%= build.bower %>/open-sans-fontface/fonts',
                        src: ['**/*.{otf,eot,svg,ttf,woff}'],
                        dest: '<%= build.dest %>/fonts/open-sans/'
                    },
                    {
                        expand: true,
                        cwd: '<%= build.bower %>/font-awesome/fonts',
                        src: ['**/*'],
                        dest: '<%= build.dest %>/fonts/font-awesome/'
                    },
                    {
                        expand: true,
                        cwd: 'src/images',
                        src: ['**/*'],
                        dest: '<%= build.dest %>/images/'
                    }
                ]
            },

            classpath: { // Copy resources to classpath so Wicket will pick them up 
                expand: true,
                cwd: '<%= build.dest %>',
                src: ['**'],
                dest: '<%= build.cp %>/skin/hippo-cms/',
                filter: function() {
                    //little hack to force it to only copy when dest exists
                    return classPathExists(); 
                }
            }
        },

        clean: {
            // clean target folder
            target: {
                src: '<%= build.target %>'
            },

            // clean bower components
            bower: {
                src: '<%= build.bower %>/**'
            },
            
            all: {
                src: ['<%= build.target %>', '<%= build.bower%>/**']
            }
        },


        // Execute shell commands
        shell: {
            options: {
                stdout: true,
                stderr: true
            },

            // Notify user when reloading. Currently only works on OSX with terminal-notifier installed (brew install terminal-notifier)
            notify: {
              command: "command -v terminal-notifier >/dev/null 2>&1 && terminal-notifier -group 'Hippo CMS' -title 'Grunt build' -subtitle 'Finished' -message 'LiveReloading' || echo 'done'"
            }
        }
    });

    grunt.registerTask('default', ['install', 'watch']);

    // build theme
    grunt.registerTask('build', 'Build the theme', [
        'less',
        'autoprefixer',
        //'csslint',
        'concat',
        'cssmin:theme',
        'copy:binaries',
        'clean:target'
    ]);

    // install
    grunt.registerTask('install', 'Build and install the theme', [
        'build',
        'copy:classpath'
    ]);
    
};
