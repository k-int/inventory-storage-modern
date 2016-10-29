package org.folio.inventory_storage

import grails.rx.web.RxController
import grails.validation.ValidationException
import groovy.transform.CompileStatic

import static org.springframework.http.HttpStatus.*
import static rx.Observable.*
import grails.rx.web.*

@CompileStatic
class InstanceController implements RxController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        zip( Instance.list(params), Instance.count() ) { List instanceList, Number count ->
            rx.render view:"index", model:[instanceList: instanceList, instanceCount: count]
        }
    }

    def show() {
        Instance.get((Serializable)params.id)
    }

    def save() {
        rx.bindData(new Instance(), request)
                .switchMap { Instance instance ->
            if(instance.hasErrors()) {
                just(
                    rx.respond( instance.errors, view:'create')
                )
            }
            else {
                instance.save(flush:true)
                        .map { Instance savedInstance ->
                    rx.respond savedInstance, [status: CREATED, view:"show"]
                }
                .onErrorReturn { Throwable e ->
                    if(e instanceof ValidationException) {
                        rx.respond e.errors, view:'create'
                    }
                    else {
                        log.error("Error saving entity: $e.message", e)
                        return INTERNAL_SERVER_ERROR
                    }
                }
            }

        }
    }

    def update() {
        def request = this.request
        Instance.get((Serializable)params.id)
                    .switchMap { Instance instance ->
            rx.bindData( instance, request )
                    .switchMap { Instance updatedBook ->
                !updatedBook.hasErrors()? updatedBook.save() : updatedBook
            }
        }
        .map { Instance instance ->
            if(instance.hasErrors()) {
                rx.respond instance.errors, view:'edit'
            }
            else {
                rx.respond instance, [status: OK, view:"show"]
            }
        }
        .switchIfEmpty(
            just( rx.render(status: NOT_FOUND) )
        )
        .onErrorReturn { Throwable e ->
            if(e instanceof ValidationException) {
                rx.respond e.errors, view:'edit'
            }
            else {
                log.error("Error saving entity: $e.message", e)
                return INTERNAL_SERVER_ERROR
            }
        }
    }

    def delete() {
        Instance.get((Serializable)params.id)
                    .switchMap { Instance instance ->
            instance.delete()
        }
        .map {
            rx.render status: NO_CONTENT
        }
    }
}
